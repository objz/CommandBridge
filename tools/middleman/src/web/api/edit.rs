use crate::state::AppState;
use axum::{Json, extract::State};
use serde::{Deserialize, Serialize};
use serde_json::json;
use std::sync::Arc;
use tokio::sync::RwLock;

#[derive(Serialize)]
pub struct EditResponse {
    pub content: Option<String>,
}

#[derive(Deserialize)]
pub struct SetEditRequest {
    pub content: String,
}

pub async fn get_handler(State(state): State<Arc<RwLock<AppState>>>) -> Json<EditResponse> {
    let content = state.read().await.get_edit_buffer().await;
    Json(EditResponse { content })
}

pub async fn set_handler(
    State(state): State<Arc<RwLock<AppState>>>,
    Json(req): Json<SetEditRequest>,
) -> Json<serde_json::Value> {
    state.read().await.set_edit_buffer(Some(req.content)).await;
    Json(json!({
        "status": "ok",
        "message": "Edit buffer updated"
    }))
}

pub async fn submit_handler(
    State(state): State<Arc<RwLock<AppState>>>,
    Json(req): Json<SetEditRequest>,
) -> Json<serde_json::Value> {
    state.read().await.set_edit_buffer(Some(req.content)).await;
    Json(json!({
        "status": "ok",
        "message": "Edit submitted and will be applied to next message"
    }))
}
