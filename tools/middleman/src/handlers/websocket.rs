use crate::message::Message;
use crate::state::LogMessage;
use crate::state::{AppState, MiddlemanMode, OperationMode};
use anyhow::{Result, anyhow};
use futures_util::{SinkExt, StreamExt};
use native_tls::TlsAcceptor as NativeTlsAcceptor;
use std::net::SocketAddr;
use std::sync::Arc;
use tokio::net::TcpStream;
use tokio::sync::RwLock;
use tokio_native_tls::TlsAcceptor;
use tokio_tungstenite::{connect_async, connect_async_tls_with_config, Connector};
use tracing::{error, info, warn};

/// Interval for checking the send queue for held messages to forward
const SEND_QUEUE_CHECK_INTERVAL_MS: u64 = 50;

async fn send_queued_messages<S>(
    state: &Arc<RwLock<AppState>>,
    sink: &mut S,
) -> Result<()>
where
    S: SinkExt<tokio_tungstenite::tungstenite::Message> + Unpin,
    S::Error: Into<anyhow::Error>,
{
    while let Some(content) = state.read().await.pop_queued_message().await {
        let tungstenite_msg = match content {
            crate::state::MessageContent::Text(s) => {
                info!("[proxy] Sending queued text message ({} bytes)", s.len());
                tokio_tungstenite::tungstenite::Message::Text(s)
            }
            crate::state::MessageContent::Binary(b) => {
                info!("[proxy] Sending queued binary message ({} bytes)", b.len());
                tokio_tungstenite::tungstenite::Message::Binary(b.to_vec())
            }
        };
        
        sink.send(tungstenite_msg).await.map_err(|e| e.into())?;
    }
    Ok(())
}

pub async fn handle_client(
    socket: TcpStream,
    peer: SocketAddr,
    state: Arc<RwLock<AppState>>,
) -> Result<()> {
    info!("[client] {} attempting connection", peer);

    let (encryption_mode, has_tls_keys) = {
        let state_guard = state.read().await;
        let config = state_guard.connection_config.read().await;
        let mode = state_guard.current_encryption_mode();
        let has_keys = config.keystore_path.is_some() && config.keystore_password.is_some();
        (mode, has_keys)
    };

    let use_tls = encryption_mode == MiddlemanMode::Encrypted && has_tls_keys;

    if use_tls {
        info!(
            "[client] {} using TLS (encryption mode: ENCRYPTED, keys loaded: yes)",
            peer
        );
        let tls_acceptor = load_tls_acceptor(&state).await?;
        let tls_stream = tls_acceptor
            .accept(socket)
            .await
            .map_err(|e| anyhow!("TLS handshake failed: {}", e))?;
        info!("[client] {} connected (TLS)", peer);
        handle_tls_client(tls_stream, peer, state).await
    } else {
        if encryption_mode == MiddlemanMode::Encrypted && !has_tls_keys {
            warn!(
                "[client] {} encryption mode is ENCRYPTED but no TLS keys loaded, falling back to plaintext",
                peer
            );
        }
        info!(
            "[client] {} using plaintext (encryption mode: {:?}, keys loaded: {})",
            peer, encryption_mode, has_tls_keys
        );
        info!("[client] {} connected (plaintext)", peer);
        handle_plain_client(socket, peer, state).await
    }
}

async fn handle_plain_client(
    socket: TcpStream,
    peer: SocketAddr,
    state: Arc<RwLock<AppState>>,
) -> Result<()> {
    let client_ws = tokio_tungstenite::accept_async(socket).await?;
    
    // Get target URL and adjust scheme based on encryption mode
    let (target_url, use_tls_for_target) = {
        let state_guard = state.read().await;
        let config = state_guard.connection_config.read().await;
        let encryption_mode = state_guard.current_encryption_mode();
        
        let mut url = config.target_url.clone();
        let use_tls = encryption_mode == MiddlemanMode::Encrypted && 
                      config.keystore_path.is_some() && 
                      config.keystore_password.is_some();
        
        // Replace ws:// with wss:// if TLS is enabled
        if use_tls && url.starts_with("ws://") {
            url = url.replacen("ws://", "wss://", 1);
            info!("[proxy] adjusted target URL to use TLS: {}", url);
        }
        
        (url, use_tls)
    };

    info!("[proxy] connecting to {} (plaintext client)", target_url);
    
    let server_ws = if use_tls_for_target {
        // Connect with TLS to target
        let connector = Connector::NativeTls(
            native_tls::TlsConnector::builder()
                .danger_accept_invalid_certs(true)
                .build()
                .map_err(|e| anyhow!("Failed to build TLS connector: {}", e))?
        );
        
        let (ws, _) = connect_async_tls_with_config(
            &target_url,
            None,
            false,
            Some(connector),
        ).await?;
        ws
    } else {
        // Plain connection to target
        let (ws, _) = connect_async(&target_url).await?;
        ws
    };
    
    info!("[proxy] connected to {}", target_url);

    state
        .read()
        .await
        .is_connected
        .store(true, std::sync::atomic::Ordering::Relaxed);

    let (mut c_sink, mut c_stream) = client_ws.split();
    let (mut s_sink, mut s_stream) = server_ws.split();

    let state1 = Arc::clone(&state);
    let state2 = Arc::clone(&state);

    let client_to_server = tokio::spawn(async move {
        let mut interval = tokio::time::interval(tokio::time::Duration::from_millis(SEND_QUEUE_CHECK_INTERVAL_MS));
        loop {
            tokio::select! {
                msg_result = c_stream.next() => {
                    match msg_result {
                        Some(Ok(msg)) => {
                            let msg = Message::from_tungstenite(msg);

                            if let Err(e) =
                                process_and_forward(&msg, "client→server", &state1, &mut s_sink).await
                            {
                                error!("[proxy] Error processing client message: {:?}", e);
                                break;
                            }
                        }
                        Some(Err(e)) => {
                            error!("[proxy] Client message error: {:?}", e);
                            break;
                        }
                        None => {
                            // Stream closed
                            break;
                        }
                    }
                }
                _ = interval.tick() => {
                    // Check for queued messages to send
                    if let Err(e) = send_queued_messages(&state1, &mut s_sink).await {
                        error!("[proxy] Error sending queued messages: {:?}", e);
                        break;
                    }
                }
            }
        }
        let _ = s_sink.close().await;
    });

    let server_to_client = tokio::spawn(async move {
        while let Some(msg_result) = s_stream.next().await {
            match msg_result {
                Ok(msg) => {
                    let msg = Message::from_tungstenite(msg);

                    if let Err(e) =
                        process_and_forward(&msg, "server→client", &state2, &mut c_sink).await
                    {
                        error!("[proxy] Error processing server message: {:?}", e);
                        break;
                    }
                }
                Err(e) => {
                    error!("[proxy] Server message error: {:?}", e);
                    break;
                }
            }
        }
        let _ = c_sink.close().await;
    });

    let _ = tokio::try_join!(client_to_server, server_to_client);
    state
        .read()
        .await
        .is_connected
        .store(false, std::sync::atomic::Ordering::Relaxed);
    info!("[client] {} disconnected", peer);
    Ok(())
}

async fn handle_tls_client<S>(
    socket: S,
    peer: SocketAddr,
    state: Arc<RwLock<AppState>>,
) -> Result<()>
where
    S: tokio::io::AsyncRead + tokio::io::AsyncWrite + Unpin + Send + 'static,
{
    let client_ws = tokio_tungstenite::accept_async(socket).await?;
    
    // Get target URL and adjust scheme based on encryption mode
    let target_url = {
        let state_guard = state.read().await;
        let config = state_guard.connection_config.read().await;
        
        let mut url = config.target_url.clone();
        
        // Always use wss:// for TLS clients
        if url.starts_with("ws://") {
            url = url.replacen("ws://", "wss://", 1);
            info!("[proxy] adjusted target URL to use TLS: {}", url);
        }
        
        url
    };

    info!("[proxy] connecting to {} (TLS client)", target_url);
    
    // Create TLS connector for target server
    let connector = Connector::NativeTls(
        native_tls::TlsConnector::builder()
            .danger_accept_invalid_certs(true)
            .build()
            .map_err(|e| anyhow!("Failed to build TLS connector: {}", e))?
    );
    
    let (server_ws, _) = connect_async_tls_with_config(
        &target_url,
        None,
        false,
        Some(connector),
    ).await?;
    
    info!("[proxy] connected to {} via TLS", target_url);

    state
        .read()
        .await
        .is_connected
        .store(true, std::sync::atomic::Ordering::Relaxed);

    let (mut c_sink, mut c_stream) = client_ws.split();
    let (mut s_sink, mut s_stream) = server_ws.split();

    let state1 = Arc::clone(&state);
    let state2 = Arc::clone(&state);

    let client_to_server = tokio::spawn(async move {
        let mut interval = tokio::time::interval(tokio::time::Duration::from_millis(SEND_QUEUE_CHECK_INTERVAL_MS));
        loop {
            tokio::select! {
                msg_result = c_stream.next() => {
                    match msg_result {
                        Some(Ok(msg)) => {
                            let msg = Message::from_tungstenite(msg);

                            if let Err(e) =
                                process_and_forward(&msg, "client→server", &state1, &mut s_sink).await
                            {
                                error!("[proxy] Error processing client message: {:?}", e);
                                break;
                            }
                        }
                        Some(Err(e)) => {
                            error!("[proxy] Client message error: {:?}", e);
                            break;
                        }
                        None => {
                            // Stream closed
                            break;
                        }
                    }
                }
                _ = interval.tick() => {
                    // Check for queued messages to send
                    if let Err(e) = send_queued_messages(&state1, &mut s_sink).await {
                        error!("[proxy] Error sending queued messages: {:?}", e);
                        break;
                    }
                }
            }
        }
        let _ = s_sink.close().await;
    });

    let server_to_client = tokio::spawn(async move {
        while let Some(msg_result) = s_stream.next().await {
            match msg_result {
                Ok(msg) => {
                    let msg = Message::from_tungstenite(msg);

                    if let Err(e) =
                        process_and_forward(&msg, "server→client", &state2, &mut c_sink).await
                    {
                        error!("[proxy] Error processing server message: {:?}", e);
                        break;
                    }
                }
                Err(e) => {
                    error!("[proxy] Server message error: {:?}", e);
                    break;
                }
            }
        }
        let _ = c_sink.close().await;
    });

    let _ = tokio::try_join!(client_to_server, server_to_client);
    state
        .read()
        .await
        .is_connected
        .store(false, std::sync::atomic::Ordering::Relaxed);
    info!("[client] {} disconnected", peer);
    Ok(())
}

async fn load_tls_acceptor(state: &Arc<RwLock<AppState>>) -> Result<TlsAcceptor> {
    let (keystore_path, password) = {
        let state_guard = state.read().await;
        let config = state_guard.connection_config.read().await;

        let keystore_path = config
            .keystore_path
            .as_ref()
            .ok_or_else(|| anyhow!("Keystore path not configured"))?
            .clone();
        let password = config
            .keystore_password
            .as_ref()
            .ok_or_else(|| anyhow!("Keystore password not configured"))?
            .clone();

        (keystore_path, password)
    };

    info!("[tls] Loading keystore from: {}", keystore_path);
    let keystore_bytes = std::fs::read(&keystore_path)
        .map_err(|e| anyhow!("Failed to read keystore file: {}", e))?;

    info!(
        "[tls] Creating TLS identity from keystore ({} bytes)",
        keystore_bytes.len()
    );
    let identity = native_tls::Identity::from_pkcs12(&keystore_bytes, &password)
        .map_err(|e| anyhow!("Failed to load PKCS12 identity: {}", e))?;

    let tls_acceptor = NativeTlsAcceptor::new(identity)
        .map_err(|e| anyhow!("Failed to create TLS acceptor: {}", e))?;

    info!("[tls] TLS acceptor created successfully");
    Ok(TlsAcceptor::from(tls_acceptor))
}

async fn process_and_forward<S>(
    msg: &Message,
    direction: &str,
    state: &Arc<RwLock<AppState>>,
    sink: &mut S,
) -> Result<()>
where
    S: SinkExt<tokio_tungstenite::tungstenite::Message> + Unpin,
    S::Error: Into<anyhow::Error>,
{
    let state_guard = state.read().await;
    let encryption_mode = state_guard.current_encryption_mode();
    let operation_mode = state_guard.current_operation_mode();
    drop(state_guard);

    let formatted_msg = format_message(msg);
    let raw_bytes = match msg {
        Message::Binary(b) => Some(base64::Engine::encode(&base64::engine::general_purpose::STANDARD, b)),
        Message::Text(s) => Some(base64::Engine::encode(&base64::engine::general_purpose::STANDARD, s.as_bytes())),
        _ => None,
    };
    
    // Store the raw content for potential resending
    let raw_content = match msg {
        Message::Text(s) => Some(crate::state::MessageContent::Text(s.clone())),
        Message::Binary(b) => Some(crate::state::MessageContent::Binary(b.clone())),
        _ => None,
    };
    
    let timestamp = chrono::Local::now()
        .format("%Y-%m-%d %H:%M:%S%.3f")
        .to_string();

    state
        .read()
        .await
        .log_message(LogMessage {
            timestamp: timestamp.clone(),
            direction: direction.to_string(),
            encryption_mode: format!("{:?}", encryption_mode),
            operation_mode: format!("{:?}", operation_mode),
            message: formatted_msg.clone(),
            is_binary: msg.is_binary(),
            raw_bytes,
            raw_content,
        })
        .await;

    if operation_mode == OperationMode::Edit {
        info!("[proxy] {} holding message in EDIT mode", direction);
        return Ok(());
    }

    sink.send(msg.to_tungstenite())
        .await
        .map_err(|e| e.into())?;

    Ok(())
}

fn format_message(msg: &Message) -> String {
    match msg {
        Message::Text(s) => format_json_message(s),
        Message::Binary(b) => format!("(binary: {} bytes)", b.len()),
        Message::Ping => "(ping)".to_string(),
        Message::Pong => "(pong)".to_string(),
        Message::Close => "(close)".to_string(),
    }
}

pub fn format_json_message(text: &str) -> String {
    match serde_json::from_str::<serde_json::Value>(text) {
        Ok(value) => serde_json::to_string_pretty(&value).unwrap_or_else(|_| text.to_string()),
        Err(_) => text.to_string(),
    }
}
