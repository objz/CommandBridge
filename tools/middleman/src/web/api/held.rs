use crate::state::{AppState, LogMessage};
use axum::{Json, extract::State};
use serde::{Deserialize, Serialize};
use serde_json::json;
use std::sync::Arc;
use tokio::sync::RwLock;

#[derive(Serialize)]
pub struct HeldMessagesResponse {
    pub messages: Vec<LogMessage>,
}

#[derive(Deserialize)]
pub struct SendHeldMessageRequest {
    pub index: usize,
    pub content: Option<String>,
}

pub async fn get_handler(State(state): State<Arc<RwLock<AppState>>>) -> Json<HeldMessagesResponse> {
    let messages = state.read().await.get_held_messages().await;
    Json(HeldMessagesResponse { messages })
}

pub async fn send_handler(
    State(state): State<Arc<RwLock<AppState>>>,
    Json(req): Json<SendHeldMessageRequest>,
) -> Json<serde_json::Value> {
    let state_ref = state.read().await;

    let Some(msg) = state_ref.remove_held_message(req.index).await else {
        return Json(json!({
            "status": "error",
            "message": "Message not found"
        }));
    };

    let content_to_send = if let Some(edited) = req.content {
        crate::state::MessageContent::Text(edited)
    } else if let Some(raw_content) = msg.raw_content {
        raw_content
    } else {
        crate::state::MessageContent::Text(msg.message)
    };

    state_ref.queue_message_for_sending(content_to_send).await;

    Json(json!({
        "status": "ok",
        "message": "Message queued for sending"
    }))
}

pub async fn send_all(State(state): State<Arc<RwLock<AppState>>>) -> Json<serde_json::Value> {
    let state_ref = state.read().await;
    let messages = state_ref.get_held_messages().await;
    let count = messages.len();

    for msg in messages {
        if let Some(raw_content) = msg.raw_content {
            state_ref.queue_message_for_sending(raw_content).await;
        } else {
            state_ref
                .queue_message_for_sending(crate::state::MessageContent::Text(msg.message))
                .await;
        }
    }

    state_ref.clear_held_messages().await;

    Json(json!({
        "status": "ok",
        "message": format!("Sent {} messages", count),
        "count": count
    }))
}

pub async fn clear_handler(State(state): State<Arc<RwLock<AppState>>>) -> Json<serde_json::Value> {
    state.read().await.clear_held_messages().await;
    Json(json!({
        "status": "ok",
        "message": "Held messages cleared"
    }))
}
