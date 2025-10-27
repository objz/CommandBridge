use serde::{Deserialize, Serialize};
use std::collections::VecDeque;
use std::sync::atomic::{AtomicU8, Ordering};
use std::sync::{
    Arc,
    atomic::{AtomicBool, AtomicU64},
};

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "UPPERCASE")]
pub enum MiddlemanMode {
    #[serde(rename = "PLAIN")]
    Plain = 0,
    #[serde(rename = "ENCRYPTED")]
    Encrypted = 1,
}

impl MiddlemanMode {
    pub fn from_u8(v: u8) -> Self {
        match v {
            1 => MiddlemanMode::Encrypted,
            _ => MiddlemanMode::Plain,
        }
    }

    pub fn to_u8(&self) -> u8 {
        *self as u8
    }
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "UPPERCASE")]
pub enum OperationMode {
    #[serde(rename = "FORWARD")]
    Forward = 0,
    #[serde(rename = "EDIT")]
    Edit = 1,
}

impl OperationMode {
    pub fn from_u8(v: u8) -> Self {
        match v {
            1 => OperationMode::Edit,
            _ => OperationMode::Forward,
        }
    }

    pub fn to_u8(&self) -> u8 {
        *self as u8
    }
}

#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct ConnectionConfig {
    pub target_url: String,
    pub keystore_path: Option<String>,
    pub keystore_password: Option<String>,
    pub hmac_secret: Option<String>,
    pub tls_pin: Option<String>,
}

#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct LogMessage {
    pub timestamp: String,
    pub direction: String,
    pub encryption_mode: String,
    pub operation_mode: String,
    pub message: String,
    pub is_binary: bool,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub raw_bytes: Option<String>, // Base64 encoded raw bytes for encrypted view
}

pub struct AppState {
    pub encryption_mode: Arc<AtomicU8>,
    pub operation_mode: Arc<AtomicU8>,
    pub connection_config: tokio::sync::RwLock<ConnectionConfig>,
    pub message_log: tokio::sync::RwLock<VecDeque<LogMessage>>,
    pub edit_buffer: tokio::sync::RwLock<Option<String>>,
    pub held_messages: tokio::sync::RwLock<VecDeque<LogMessage>>,
    pub message_count: Arc<AtomicU64>,
    pub is_connected: Arc<AtomicBool>,
}

impl AppState {
    pub fn new(target_url: String) -> Self {
        Self {
            encryption_mode: Arc::new(AtomicU8::new(MiddlemanMode::Plain.to_u8())),
            operation_mode: Arc::new(AtomicU8::new(OperationMode::Forward.to_u8())),
            connection_config: tokio::sync::RwLock::new(ConnectionConfig {
                target_url,
                keystore_path: None,
                keystore_password: None,
                hmac_secret: None,
                tls_pin: None,
            }),
            message_log: tokio::sync::RwLock::new(VecDeque::with_capacity(1000)),
            edit_buffer: tokio::sync::RwLock::new(None),
            held_messages: tokio::sync::RwLock::new(VecDeque::new()),
            message_count: Arc::new(AtomicU64::new(0)),
            is_connected: Arc::new(AtomicBool::new(false)),
        }
    }

    pub fn current_encryption_mode(&self) -> MiddlemanMode {
        MiddlemanMode::from_u8(self.encryption_mode.load(Ordering::Relaxed))
    }

    pub fn set_encryption_mode(&self, m: MiddlemanMode) {
        tracing::info!("[state] Encryption mode changed to: {:?}", m);
        self.encryption_mode.store(m.to_u8(), Ordering::Relaxed);
    }

    pub fn current_operation_mode(&self) -> OperationMode {
        OperationMode::from_u8(self.operation_mode.load(Ordering::Relaxed))
    }

    pub fn set_operation_mode(&self, m: OperationMode) {
        tracing::info!("[state] Operation mode changed to: {:?}", m);
        self.operation_mode.store(m.to_u8(), Ordering::Relaxed);
    }

    pub async fn log_message(&self, message: LogMessage) {
        let mut log = self.message_log.write().await;
        if log.len() >= 1000 {
            log.pop_front();
        }
        tracing::debug!(
            "[log] {} {} [{}|{}] {}",
            message.timestamp,
            message.direction,
            message.encryption_mode,
            message.operation_mode,
            if message.is_binary {
                format!("(binary: {} bytes)", message.message.len())
            } else {
                message.message.chars().take(100).collect::<String>()
            }
        );

        if self.current_operation_mode() == OperationMode::Edit {
            let mut held = self.held_messages.write().await;
            held.push_back(message.clone());
            tracing::debug!("[state] Message held, total held: {}", held.len());
        }

        log.push_back(message);
        self.message_count.fetch_add(1, Ordering::Relaxed);
    }

    pub async fn get_logs(&self) -> Vec<LogMessage> {
        self.message_log.read().await.iter().cloned().collect()
    }

    pub async fn get_held_messages(&self) -> Vec<LogMessage> {
        self.held_messages.read().await.iter().cloned().collect()
    }

    pub async fn clear_logs(&self) {
        tracing::info!("[state] Clearing message logs");
        self.message_log.write().await.clear();
        self.held_messages.write().await.clear();
    }

    pub async fn clear_held_messages(&self) {
        tracing::info!("[state] Clearing held messages");
        self.held_messages.write().await.clear();
    }

    pub async fn remove_held_message(&self, index: usize) -> Option<LogMessage> {
        let mut held = self.held_messages.write().await;
        if index < held.len() {
            Some(held.remove(index).unwrap())
        } else {
            None
        }
    }

    pub async fn set_edit_buffer(&self, content: Option<String>) {
        if let Some(ref c) = content {
            tracing::info!("[state] Edit buffer set ({} bytes)", c.len());
        } else {
            tracing::info!("[state] Edit buffer cleared");
        }
        *self.edit_buffer.write().await = content;
    }

    pub async fn get_edit_buffer(&self) -> Option<String> {
        self.edit_buffer.read().await.clone()
    }
}
