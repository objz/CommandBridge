mod handlers;
mod message;
mod state;
mod web;

use anyhow::Result;
use std::{net::SocketAddr, sync::Arc};
use tokio::sync::RwLock;
use tracing::error;
use tracing_subscriber::EnvFilter;

use crate::state::AppState;

#[tokio::main]
async fn main() -> Result<()> {
    install_tracing();

    //INFO: may want to change that later but for testing local it's fine

    const PROXY_BIND: &str = "127.0.0.1:8766";
    const WEB_BIND: &str = "127.0.0.1:8080";
    const DEFAULT_TARGET: &str = "ws://127.0.0.1:8765/ws";

    let proxy_addr: SocketAddr = PROXY_BIND.parse()?;
    let web_addr: SocketAddr = WEB_BIND.parse()?;

    let app_state = Arc::new(RwLock::new(AppState::new(DEFAULT_TARGET.to_string())));

    let proxy_state = Arc::clone(&app_state);
    let proxy_task = tokio::spawn(async move {
        if let Err(e) = handlers::proxy::run_proxy(proxy_addr, proxy_state).await {
            error!("[proxy] fatal error: {:?}", e);
        }
    });

    let web_state = Arc::clone(&app_state);
    let web_task = tokio::spawn(async move {
        if let Err(e) = web::run_web_server(web_addr, web_state).await {
            error!("[web] fatal error: {:?}", e);
        }
    });

    tokio::try_join!(proxy_task, web_task)?;
    Ok(())
}

fn install_tracing() {
    let filter = EnvFilter::try_from_default_env().unwrap_or_else(|_| EnvFilter::new("info"));
    tracing_subscriber::fmt().with_env_filter(filter).init();
}
