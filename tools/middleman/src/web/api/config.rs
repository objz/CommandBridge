use axum::{extract::State, Json};
use serde::{Deserialize, Serialize};
use serde_json::json;
use std::sync::Arc;
use tokio::sync::RwLock;
use crate::state::AppState;

#[derive(Serialize, Deserialize)]
pub struct ConfigResponse {
    pub target_url: String,
    pub keystore_path: Option<String>,
    pub hmac_secret: Option<String>,
    pub tls_pin: Option<String>,
}

#[derive(Deserialize)]
pub struct SetConfigRequest {
    pub target_url: Option<String>,
    pub keystore_path: Option<String>,
    pub keystore_password: Option<String>,
    pub hmac_secret: Option<String>,
    pub tls_pin: Option<String>,
}

pub async fn get_handler(State(state): State<Arc<RwLock<AppState>>>) -> Json<ConfigResponse> {
    let state_ref = state.read().await;
    let config = state_ref.connection_config.read().await;
    Json(ConfigResponse {
        target_url: config.target_url.clone(),
        keystore_path: config.keystore_path.clone(),
        hmac_secret: if config.hmac_secret.is_some() {
            Some("***REDACTED***".to_string())
        } else {
            None
        },
        tls_pin: config.tls_pin.clone(),
    })
}

pub async fn set_handler(
    State(state): State<Arc<RwLock<AppState>>>,
    Json(req): Json<SetConfigRequest>,
) -> Json<serde_json::Value> {
    let state_ref = state.read().await;
    let mut config = state_ref.connection_config.write().await;

    if let Some(url) = req.target_url {
        config.target_url = url;
    }
    if let Some(path) = req.keystore_path {
        config.keystore_path = Some(path);
    }
    if let Some(password) = req.keystore_password {
        config.keystore_password = Some(password);
    }
    if let Some(secret) = req.hmac_secret {
        config.hmac_secret = Some(secret);
    }
    if let Some(pin) = req.tls_pin {
        config.tls_pin = Some(pin);
    }

    Json(json!({
        "status": "ok",
        "message": "Configuration updated"
    }))
}
