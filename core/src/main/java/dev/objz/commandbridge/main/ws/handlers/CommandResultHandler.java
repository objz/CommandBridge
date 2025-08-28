
package dev.objz.commandbridge.main.ws.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.objz.commandbridge.main.logging.Log;
import dev.objz.commandbridge.main.proto.Envelope;
import dev.objz.commandbridge.main.proto.cmd.CommandResultPayload;
import dev.objz.commandbridge.main.ws.SessionHub;
import io.undertow.websockets.core.WebSocketChannel;

public final class CommandResultHandler implements ServerMessageHandler {
    private final ObjectMapper mapper;
    private final SessionHub sessions;

    public CommandResultHandler(ObjectMapper mapper, SessionHub sessions) {
        this.mapper = mapper;
        this.sessions = sessions;
    }

    @Override
    public void handle(WebSocketChannel ch, Envelope env) throws Exception {
        if (!sessions.all().stream().anyMatch(s -> s.ch() == ch && s.authed())) {
            Log.warn("Dropping COMMAND_RESULT from unauthenticated channel {}", ch.getSourceAddress());
            return;
        }
        CommandResultPayload p = mapper.treeToValue(env.payload(), CommandResultPayload.class);
        Log.info("[result] planId={} target={} ok={} ms={} err='{}' out='{}'",
                p.planId(), p.targetBackendId(), p.success(), p.durationMs(), p.error(), p.output());
        // Later: correlate by planId and aggregate
    }
}
