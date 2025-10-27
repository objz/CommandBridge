use crate::state::{AppState, LogMessage};
use axum::{Json, extract::State};
use serde_json::json;
use std::sync::Arc;
use tokio::sync::RwLock;

pub async fn get_handler(State(state): State<Arc<RwLock<AppState>>>) -> Json<Vec<LogMessage>> {
    let logs = state.read().await.get_logs().await;
    Json(logs)
}

pub async fn clear_handler(State(state): State<Arc<RwLock<AppState>>>) -> Json<serde_json::Value> {
    state.read().await.clear_logs().await;
    Json(json!({
        "status": "ok",
        "message": "Logs cleared"
    }))
}
