use crate::state::{AppState, MiddlemanMode, OperationMode};
use axum::{Json, extract::State};
use serde::{Deserialize, Serialize};
use serde_json::json;
use std::sync::Arc;
use tokio::sync::RwLock;

#[derive(Deserialize)]
pub struct SetModeRequest {
    pub encryption_mode: Option<String>,
    pub operation_mode: Option<String>,
}

#[derive(Serialize)]
pub struct ModeResponse {
    pub encryption_mode: String,
    pub operation_mode: String,
}

pub async fn get_handler(State(state): State<Arc<RwLock<AppState>>>) -> Json<ModeResponse> {
    let state = state.read().await;
    Json(ModeResponse {
        encryption_mode: format!("{:?}", state.current_encryption_mode()),
        operation_mode: format!("{:?}", state.current_operation_mode()),
    })
}

pub async fn set_handler(
    State(state): State<Arc<RwLock<AppState>>>,
    Json(req): Json<SetModeRequest>,
) -> Json<serde_json::Value> {
    let state_guard = state.read().await;

    if let Some(enc_mode_str) = req.encryption_mode {
        let mode = match enc_mode_str.to_lowercase().as_str() {
            "plain" => MiddlemanMode::Plain,
            "encrypted" => MiddlemanMode::Encrypted,
            _ => {
                return Json(json!({
                    "status": "error",
                    "message": "Invalid encryption mode. Use: plain or encrypted"
                }));
            }
        };
        state_guard.set_encryption_mode(mode);
    }

    if let Some(op_mode_str) = req.operation_mode {
        let mode = match op_mode_str.to_lowercase().as_str() {
            "forward" => OperationMode::Forward,
            "edit" => OperationMode::Edit,
            _ => {
                return Json(json!({
                    "status": "error",
                    "message": "Invalid operation mode. Use: forward or edit"
                }));
            }
        };
        state_guard.set_operation_mode(mode);
    }

    Json(json!({
        "status": "ok",
        "encryption_mode": format!("{:?}", state_guard.current_encryption_mode()),
        "operation_mode": format!("{:?}", state_guard.current_operation_mode()),
    }))
}
