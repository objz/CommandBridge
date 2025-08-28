package dev.objz.commandbridge.main.ws.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.objz.commandbridge.main.logging.Log;
import dev.objz.commandbridge.main.proto.Envelope;
import dev.objz.commandbridge.main.proto.cmd.CommandInvokedPayload;
import dev.objz.commandbridge.main.ws.SessionHub;
import io.undertow.websockets.core.WebSocketChannel;

public final class CommandInvokedHandler implements ServerMessageHandler {
    private final ObjectMapper mapper;
    private final SessionHub sessions;

    public CommandInvokedHandler(ObjectMapper mapper, SessionHub sessions) {
        this.mapper = mapper;
        this.sessions = sessions;
    }

    @Override
    public void handle(WebSocketChannel ch, Envelope env) throws Exception {
        // basic auth gate
        if (!sessions.all().stream().anyMatch(s -> s.ch() == ch && s.authed())) {
            Log.warn("Dropping COMMAND_INVOKED from unauthenticated channel {}", ch.getSourceAddress());
            return;
        }
        CommandInvokedPayload p = mapper.treeToValue(env.payload(), CommandInvokedPayload.class);
        // For now: just log receipt (planning/validation comes next phase)
        Log.info("[invoked] cmdId={} alias={} backend={} exec={} ({}) args={}",
                p.commandId(), p.alias(), p.backendId(), p.executorName(), p.executorUuid(), p.rawArgs());
        // Later phases: build ExecutionPlan and send EXECUTE_COMMAND per step
    }
}
