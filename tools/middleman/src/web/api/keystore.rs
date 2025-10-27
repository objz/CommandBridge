use crate::state::AppState;
use axum::{Json, extract::State};
use base64::Engine;
use serde_json::json;
use std::sync::Arc;
use tokio::sync::RwLock;

pub async fn load_keystore(
    State(state): State<Arc<RwLock<AppState>>>,
    Json(req): Json<serde_json::Value>,
) -> Json<serde_json::Value> {
    let keystore_path = req
        .get("keystore_path")
        .and_then(|v| v.as_str())
        .unwrap_or("");
    let password = req.get("password").and_then(|v| v.as_str()).unwrap_or("");

    if keystore_path.is_empty() || password.is_empty() {
        return Json(json!({
            "status": "error",
            "message": "keystore_path and password are required"
        }));
    }

    match std::fs::read(keystore_path) {
        Ok(keystore_bytes) => match native_tls::Identity::from_pkcs12(&keystore_bytes, password) {
            Ok(_) => {
                let state_ref = state.read().await;
                let mut config = state_ref.connection_config.write().await;
                config.keystore_path = Some(keystore_path.to_string());
                config.keystore_password = Some(password.to_string());

                Json(json!({
                    "status": "ok",
                    "message": "Keystore loaded successfully",
                }))
            }
            Err(e) => Json(json!({
                "status": "error",
                "message": format!("Failed to load PKCS12 keystore: {}", e)
            })),
        },
        Err(e) => Json(json!({
            "status": "error",
            "message": format!("Failed to read keystore file: {}", e)
        })),
    }
}

pub async fn load_keystore_base64(
    State(state): State<Arc<RwLock<AppState>>>,
    Json(req): Json<serde_json::Value>,
) -> Json<serde_json::Value> {
    let keystore_base64 = req
        .get("keystore_base64")
        .and_then(|v| v.as_str())
        .unwrap_or("");
    let password = req.get("password").and_then(|v| v.as_str()).unwrap_or("");

    if keystore_base64.is_empty() || password.is_empty() {
        return Json(json!({
            "status": "error",
            "message": "keystore_base64 and password are required"
        }));
    }

    let keystore_bytes =
        match Engine::decode(&base64::engine::general_purpose::STANDARD, keystore_base64) {
            Ok(bytes) => bytes,
            Err(e) => {
                return Json(json!({
                    "status": "error",
                    "message": format!("Failed to decode base64: {}", e)
                }));
            }
        };

    match native_tls::Identity::from_pkcs12(&keystore_bytes, password) {
        Ok(_) => {
            let temp_path = "/tmp/middleman_keystore.p12";
            match std::fs::write(temp_path, &keystore_bytes) {
                Ok(_) => {
                    let state_ref = state.read().await;
                    let mut config = state_ref.connection_config.write().await;
                    config.keystore_path = Some(temp_path.to_string());
                    config.keystore_password = Some(password.to_string());

                    Json(json!({
                        "status": "ok",
                        "message": "Keystore loaded successfully",
                    }))
                }
                Err(e) => Json(json!({
                    "status": "error",
                    "message": format!("Failed to save keystore: {}", e)
                })),
            }
        }
        Err(e) => Json(json!({
            "status": "error",
            "message": format!("Failed to load PKCS12 keystore: {}", e)
        })),
    }
}
