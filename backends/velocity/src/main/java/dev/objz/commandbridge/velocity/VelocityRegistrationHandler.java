package dev.objz.commandbridge.velocity;

import com.velocitypowered.api.proxy.ProxyServer;
import dev.objz.commandbridge.backends.net.client.BackendClient;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.logging.Summary;
import dev.objz.commandbridge.net.Endpoint;
import dev.objz.commandbridge.net.InboundHandler;
import dev.objz.commandbridge.net.payloads.cmd.CommandStub;
import dev.objz.commandbridge.net.payloads.cmd.RegisterCommands;
import dev.objz.commandbridge.net.payloads.feedback.Feedback;
import dev.objz.commandbridge.net.payloads.feedback.FeedbackCollector;
import dev.objz.commandbridge.net.proto.Envelope;
import dev.objz.commandbridge.net.proto.MessageType;

import java.util.List;
import java.util.Objects;

final class VelocityRegistrationHandler extends InboundHandler {

    private final BackendClient client;
    private final VelocityCommandRegistry registry;
    private volatile List<CommandStub> registeredCommands = List.of();

    VelocityRegistrationHandler(BackendClient client, ProxyServer proxy) {
        this.client = Objects.requireNonNull(client);
        this.registry = new VelocityCommandRegistry(Objects.requireNonNull(proxy),
                client.outboundRouter());
    }

    @Override
    public void accept(Endpoint endpoint, Envelope env) {
        RegisterCommands rc = null;
        try {
            rc = Envelope.MAPPER.treeToValue(env.payload(), RegisterCommands.class);
        } catch (Exception e) {
            Log.error(e, "Failed to handle REGISTER_COMMANDS from {}", env.from());
        }

        try {
            registry.unregisterAll();
        } catch (Exception e) {
            Log.warn("Failed to unregister previous commands: {}", e.getMessage());
        }

        if (rc == null || rc.commands() == null || rc.commands().isEmpty()) {
            Log.warn("Received empty or malformed REGISTER_COMMANDS. All commands unregistered");
            registeredCommands = List.of();

            Feedback f = new Feedback(0, 0, 0,
                    List.of("Received empty registration request"),
                    List.of());
            reply(endpoint, env, MessageType.REGISTER_COMMANDS_RESULT, f)
                    .dispatch()
                    .exceptionally(ex -> {
                        Log.warn("Failed to send FEEDBACK: {}", ex.toString());
                        return null;
                    });
            return;
        }

        FeedbackCollector fc = new FeedbackCollector();
        for (CommandStub stub : rc.commands()) {
            try {
                registry.register(stub);
                fc.success();
            } catch (Throwable t) {
                Log.error(t, "Registration failed for '{}'", stub != null ? stub.name() : "<null>");
                fc.failure("register '" + (stub != null ? stub.name() : "<null>") + "': "
                        + t.getMessage());
            }
        }

        registeredCommands = List.copyOf(rc.commands());
        client.setServerId(env.from());

        Feedback feedback = fc.build();
        Summary.feedbackSummary("Registration", feedback, env.from());
        Summary.feedbackDetails(feedback, env.from(), true);

        reply(endpoint, env, MessageType.REGISTER_COMMANDS_RESULT, feedback)
                .dispatch()
                .exceptionally(ex -> {
                    Log.warn("Failed to send FEEDBACK: {}", ex.toString());
                    return null;
                });
    }

    List<CommandStub> snapshotRegisteredCommands() {
        return List.copyOf(registeredCommands);
    }
}
