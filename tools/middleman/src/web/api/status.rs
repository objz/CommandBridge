use crate::state::AppState;
use axum::{Json, extract::State};
use serde_json::json;
use std::sync::Arc;
use tokio::sync::RwLock;

pub async fn handler(State(state): State<Arc<RwLock<AppState>>>) -> Json<serde_json::Value> {
    let state = state.read().await;
    let config = state.connection_config.read().await;

    Json(json!({
        "encryption_mode": format!("{:?}", state.current_encryption_mode()),
        "operation_mode": format!("{:?}", state.current_operation_mode()),
        "connected": state.is_connected.load(std::sync::atomic::Ordering::Relaxed),
        "message_count": state.message_count.load(std::sync::atomic::Ordering::Relaxed),
        "target_url": config.target_url,
        "has_tls_config": config.keystore_path.is_some() && config.keystore_password.is_some(),
    }))
}
