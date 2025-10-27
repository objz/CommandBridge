use crate::state::AppState;
use anyhow::Result;
use std::net::SocketAddr;
use std::sync::Arc;
use tokio::net::TcpListener;
use tokio::sync::RwLock;
use tracing::{error, info};

pub async fn run_proxy(bind: SocketAddr, state: Arc<RwLock<AppState>>) -> Result<()> {
    let listener = TcpListener::bind(bind).await?;
    info!("[proxy] listening on {}", bind);

    loop {
        let (socket, peer) = listener.accept().await?;
        let state_clone = Arc::clone(&state);

        tokio::spawn(async move {
            if let Err(e) = crate::handlers::websocket::handle_client(socket, peer, state_clone).await {
                error!("[proxy] client {} error: {:?}", peer, e);
            }
        });
    }
}
