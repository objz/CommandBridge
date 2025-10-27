use axum::extract::ws::{WebSocket, WebSocketUpgrade};
use std::sync::Arc;
use tokio::sync::RwLock;
use crate::state::AppState;

pub async fn logs_handler(ws: WebSocketUpgrade, axum::extract::State(state): axum::extract::State<Arc<RwLock<AppState>>>) -> impl axum::response::IntoResponse {
    ws.on_upgrade(|socket| handle_logs(socket, state))
}

async fn handle_logs(mut socket: WebSocket, state: Arc<RwLock<AppState>>) {
    let _ = socket.send(axum::extract::ws::Message::Text(
        "[Connected to middleman]\n".into(),
    )).await;

    let mut interval = tokio::time::interval(std::time::Duration::from_millis(500));

    loop {
        tokio::select! {
            _ = interval.tick() => {
                let logs = state.read().await.get_logs().await;
                if let Ok(msg_str) = serde_json::to_string(&logs) {
                    if socket.send(axum::extract::ws::Message::Text(msg_str)).await.is_err() {
                        break;
                    }
                }
            }
            msg = socket.recv() => {
                if msg.is_none() {
                    break;
                }
            }
        }
    }
}
