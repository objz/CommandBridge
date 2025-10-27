mod api;
mod static_files;
mod ws;

use anyhow::Result;
use axum::{
    Router,
    extract::DefaultBodyLimit,
    http::{StatusCode, header},
    response::{Html, IntoResponse},
    routing::{get, post},
};
use std::net::SocketAddr;
use std::sync::Arc;
use tokio::sync::RwLock;
use tower_http::cors::CorsLayer;

use crate::state::AppState;

pub async fn run_web_server(bind: SocketAddr, state: Arc<RwLock<AppState>>) -> Result<()> {
    tracing::info!("[web] starting on {}", bind);

    let app = Router::new()
        // static files
        .route("/", get(serve_index))
        .route("/static/styles.css", get(serve_css))
        .route("/static/app.js", get(serve_js))
        // API endpoints
        .route("/api/status", get(api::status::handler))
        .route("/api/mode", get(api::mode::get_handler))
        .route("/api/mode", post(api::mode::set_handler))
        .route("/api/config", get(api::config::get_handler))
        .route("/api/config", post(api::config::set_handler))
        .route("/api/logs", get(api::logs::get_handler))
        .route("/api/logs", post(api::logs::clear_handler))
        .route("/api/edit", get(api::edit::get_handler))
        .route("/api/edit", post(api::edit::set_handler))
        .route("/api/edit/submit", post(api::edit::submit_handler))
        .route("/api/held", get(api::held::get_handler))
        .route("/api/held/send", post(api::held::send_handler))
        .route("/api/held/send-all", post(api::held::send_all))
        .route("/api/held/clear", post(api::held::clear_handler))
        .route(
            "/api/keystore/load-direct",
            post(api::keystore::load_keystore),
        )
        .route(
            "/api/keystore/load-base64",
            post(api::keystore::load_keystore_base64),
        )
        // ws endpoint
        .route("/ws/logs", get(ws::logs_handler))
        // 404
        .fallback(get(|| async { StatusCode::NOT_FOUND }))
        .layer(DefaultBodyLimit::max(100 * 1024 * 1024))
        .layer(CorsLayer::permissive())
        .with_state(state);

    let listener = tokio::net::TcpListener::bind(&bind).await?;
    axum::serve(listener, app).await?;
    Ok(())
}

async fn serve_index() -> Html<&'static str> {
    Html(static_files::index::INDEX_HTML)
}

async fn serve_css() -> impl IntoResponse {
    (
        [(header::CONTENT_TYPE, "text/css; charset=utf-8")],
        static_files::index::STYLES_CSS,
    )
}

async fn serve_js() -> impl IntoResponse {
    (
        [(
            header::CONTENT_TYPE,
            "application/javascript; charset=utf-8",
        )],
        static_files::index::APP_JS,
    )
}
