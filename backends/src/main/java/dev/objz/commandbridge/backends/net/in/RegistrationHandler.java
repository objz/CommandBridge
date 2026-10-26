package dev.objz.commandbridge.backends.net.in;

import dev.objz.commandbridge.backends.net.client.BackendClient;
import dev.objz.commandbridge.backends.platform.cmd.ArgumentMapper;
import dev.objz.commandbridge.backends.platform.cmd.CommandRegistry;
import dev.objz.commandbridge.cmd.CommandRegistryInterface;
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

public final class RegistrationHandler extends InboundHandler {
    private final BackendClient client;
    private final CommandRegistryInterface registry;
    private volatile List<CommandStub> registeredCommands = List.of();

    public RegistrationHandler(BackendClient client) {
        this(client, new CommandRegistry(new ArgumentMapper(), client.outboundRouter()));
    }

    RegistrationHandler(BackendClient client, CommandRegistryInterface registry) {
        this.client = Objects.requireNonNull(client);
        this.registry = Objects.requireNonNull(registry);
    }

    /** Drops the commands this handler registered, before a new client takes over. */
    public void unregisterAll() {
        try {
            registry.unregisterAll();
        } catch (Exception e) {
            Log.warn("Failed to unregister previous commands: {}", e.getMessage());
        }
        registeredCommands = List.of();
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
            if (rc == null || rc.commands() == null)
                Log.warn("Received malformed REGISTER_COMMANDS. All commands unregistered");
            else
                Log.debug("The proxy has no commands for this server");
            registeredCommands = List.of();
            Feedback f = new Feedback(0, 0, 0,
                    List.of("Received empty registration request"),
                    List.of());
            reply(endpoint, env, MessageType.REGISTER_COMMANDS_RESULT, f).dispatch().exceptionally(ex -> {
                Log.warn("Failed to send FEEDBACK: {}", ex.toString());
                return null;
            });
            return;
        }

        FeedbackCollector fc = new FeedbackCollector();
        for (CommandStub s : rc.commands()) {
            try {
                registry.register(s);
                fc.success();
            } catch (Throwable t) {
                Log.error(t, "Registration failed for '{}'", s != null ? s.name() : "<null>");
                fc.failure("register '" + (s != null ? s.name() : "<null>") + "': "
                        + t.getMessage());
            }
        }
        registeredCommands = List.copyOf(rc.commands());
        if (client.serverId() == null || client.serverId().isBlank()) {
            client.setServerId(env.from());
        }
        Feedback f = fc.build();
        Summary.feedbackSummary("Registration", f, env.from());
        Summary.feedbackDetails(f, env.from(), true);

        reply(endpoint, env, MessageType.REGISTER_COMMANDS_RESULT, f).dispatch().exceptionally(ex -> {
            Log.warn("Failed to send FEEDBACK: {}", ex.toString());
            return null;
        });
    }

    public List<CommandStub> snapshotRegisteredCommands() {
        return List.copyOf(registeredCommands);
    }
}
