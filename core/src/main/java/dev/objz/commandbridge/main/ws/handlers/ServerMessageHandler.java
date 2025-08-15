package dev.objz.commandbridge.main.ws.handlers;

import dev.objz.commandbridge.main.proto.Envelope;
import io.undertow.websockets.core.WebSocketChannel;

public interface ServerMessageHandler {
	void handle(WebSocketChannel ch, Envelope env) throws Exception;
}
