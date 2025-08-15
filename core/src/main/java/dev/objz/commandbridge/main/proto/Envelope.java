package dev.objz.commandbridge.main.proto;

import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public record Envelope(int v, UUID id, MessageType type, String from, String to, long ts, JsonNode payload) {

	private static final ObjectMapper M = new ObjectMapper();
	public static Envelope ping(String from) {
		return new Envelope(1, UUID.randomUUID(), MessageType.PING, from, null, System.currentTimeMillis(),
				M.nullNode());
	}

	public static Envelope pong(Envelope ping, String from) {
		return new Envelope(ping.v(), ping.id(), MessageType.PONG, from, ping.from(),
				System.currentTimeMillis(), M.nullNode());
	}

	public static Envelope make(MessageType type, String from, String to, JsonNode payload) {
		return new Envelope(1, UUID.randomUUID(), type, from, to, System.currentTimeMillis(),
				payload != null ? payload : M.nullNode());
	}
}
