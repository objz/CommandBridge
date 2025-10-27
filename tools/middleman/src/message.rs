use bytes::Bytes;

#[derive(Debug, Clone)]
pub enum Message {
    Text(String),
    Binary(Bytes),
    Ping,
    Pong,
    Close,
}

impl Message {
    pub fn is_binary(&self) -> bool {
        matches!(self, Message::Binary(_))
    }

    pub fn to_tungstenite(&self) -> tungstenite::Message {
        match self {
            Message::Text(s) => tungstenite::Message::Text(s.clone()),
            Message::Binary(b) => tungstenite::Message::Binary(b.to_vec()),
            Message::Ping => tungstenite::Message::Ping(vec![]),
            Message::Pong => tungstenite::Message::Pong(vec![]),
            Message::Close => tungstenite::Message::Close(None),
        }
    }

    pub fn from_tungstenite(msg: tungstenite::Message) -> Self {
        match msg {
            tungstenite::Message::Text(s) => Message::Text(s),
            tungstenite::Message::Binary(b) => Message::Binary(Bytes::from(b)),
            tungstenite::Message::Ping(_) => Message::Ping,
            tungstenite::Message::Pong(_) => Message::Pong,
            tungstenite::Message::Close(_) => Message::Close,
            tungstenite::Message::Frame(_) => Message::Close,
        }
    }
}
