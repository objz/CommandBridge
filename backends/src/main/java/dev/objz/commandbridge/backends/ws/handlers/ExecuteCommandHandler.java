package dev.objz.commandbridge.backends.ws.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.objz.commandbridge.backends.ws.IncomingDispatcher.InboundHandler;
import dev.objz.commandbridge.backends.ws.WsClient;
import dev.objz.commandbridge.main.logging.Log;
import dev.objz.commandbridge.main.proto.Envelope;
import dev.objz.commandbridge.main.proto.MessageType;
import dev.objz.commandbridge.main.proto.cmd.CommandResultPayload;
import dev.objz.commandbridge.main.proto.cmd.ExecuteCommandPayload;

public final class ExecuteCommandHandler implements InboundHandler {
    private final WsClient ws;
    private final ObjectMapper mapper;

    public ExecuteCommandHandler(WsClient ws, ObjectMapper mapper) {
        this.ws = ws;
        this.mapper = mapper;
    }

    @Override
    public void handle(Envelope env) throws Exception {
        ExecuteCommandPayload p = mapper.treeToValue(env.payload(), ExecuteCommandPayload.class);
        Log.info("EXECUTE_COMMAND planId={} target={} as={} runAs={} cmd='{}' timeoutMs={}",
                p.planId(), p.targetBackendId(), p.asMode(), p.runAs(), p.command(), p.timeoutMs());

        // Placeholder executor (no-op) — we immediately reply "not implemented yet"
        var result = new CommandResultPayload(
                p.planId(),
                p.targetBackendId(),
                false,
                "",
                "Not implemented on backend yet",
                0L
        );
        ObjectNode payload = mapper.valueToTree(result);
        Envelope reply = Envelope.reply(env, MessageType.COMMAND_RESULT, ws.clientId(), payload);
        ws.send(reply);
    }
}
