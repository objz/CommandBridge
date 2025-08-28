package dev.objz.commandbridge.backends.ws.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.objz.commandbridge.backends.ws.IncomingDispatcher.InboundHandler;
import dev.objz.commandbridge.backends.ws.WsClient;
import dev.objz.commandbridge.main.logging.Log;
import dev.objz.commandbridge.main.proto.Envelope;
import dev.objz.commandbridge.main.proto.cmd.CommandStub;
import dev.objz.commandbridge.main.proto.cmd.RegisterCommandsPayload;

import java.util.List;

public final class RegisterCommandsHandler implements InboundHandler {
    private final WsClient ws;
    private final ObjectMapper mapper;

    public RegisterCommandsHandler(WsClient ws, ObjectMapper mapper) {
        this.ws = ws;
        this.mapper = mapper;
    }

    @Override
    public void handle(Envelope env) throws Exception {
        RegisterCommandsPayload p = mapper.treeToValue(env.payload(), RegisterCommandsPayload.class);
        List<CommandStub> stubs = p.commands();
        Log.info("Received {} command stubs from proxy", stubs.size());
        for (CommandStub s : stubs) {
            Log.debug("  - {} (aliases: {}) :: {}", s.primary(), String.join(",", s.aliases()), s.usage());
        }
        // NOTE: actual Bukkit/Paper registration will be implemented next step.
        // For now we just cache them if you want:
        // ws.setLatestStubs(stubs); // add a field/method if you like
    }
}
