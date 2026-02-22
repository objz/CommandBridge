package dev.objz.commandbridge.velocity.net.in;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.net.Endpoint;
import dev.objz.commandbridge.net.InboundHandler;
import dev.objz.commandbridge.net.payloads.cmd.ExecuteCommandResult;
import dev.objz.commandbridge.net.payloads.feedback.Feedback;
import dev.objz.commandbridge.net.proto.Envelope;
import dev.objz.commandbridge.logging.Summary;
import dev.objz.commandbridge.util.MM;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class ExecuteCommandHandler extends InboundHandler {

    private final ProxyServer proxy;

    public ExecuteCommandHandler(ProxyServer proxy) {
        this.proxy = Objects.requireNonNull(proxy);
    }

    @Override
    public void accept(Endpoint endpoint, Envelope env) {
        if (env.payload() == null) {
            Log.warn("Received EXECUTE_COMMAND_RESULT with null payload from '{}'", env.from());
            return;
        }

        ExecuteCommandResult result;
        try {
            result = Envelope.MAPPER.treeToValue(env.payload(), ExecuteCommandResult.class);
        } catch (Exception e) {
            Log.error(e, "Failed to parse EXECUTE_COMMAND_RESULT from '{}'", env.from());
            return;
        }

        if (result == null) {
            Log.warn("Parsed EXECUTE_COMMAND_RESULT is null from '{}'", env.from());
            return;
        }

        String clientId = env.from();

        if (result.success()) {
            Log.debug("Command '{}' executed successfully on '{}'", result.command(), clientId);
        } else {
            Feedback feedback = new Feedback(
                    1, // requested
                    0, // succeeded
                    1, // failed
                    List.of(), // warnings
                    result.errors() != null ? result.errors() : List.of(result.message()));

            Summary.feedbackSummary("Execution Failed", feedback, clientId);
            Summary.feedbackDetails(feedback, clientId, false);

            notifyPlayer(result.playerUuid(), result.command(), result.message(), clientId);
        }
    }

    private void notifyPlayer(UUID playerUuid, String command, String errorMessage, String clientId) {
        if (playerUuid == null) {
            return;
        }

        Optional<Player> playerOpt = proxy.getPlayer(playerUuid);
        if (playerOpt.isEmpty()) {
            Log.debug("Cannot notify player {} - not online on proxy", playerUuid);
            return;
        }

        Player player = playerOpt.get();

        player.sendMessage(MM.parse("<red>⚠</red> <gray>Command execution failed</gray>"));
        player.sendMessage(
                MM.parse("<dark_gray>If this persists, please contact an administrator</dark_gray>"));

        if (player.hasPermission("commandbridge.admin")) {
            player.sendMessage(MM.parse("<dark_gray>Command: </dark_gray><white>" + command + "</white>"));
            player.sendMessage(MM.parse("<dark_gray>Backend: </dark_gray><white>" + clientId + "</white>"));
            if (errorMessage != null) {
                player.sendMessage(MM.parse(
                        "<dark_gray>Error:  </dark_gray><red>" + errorMessage + "</red>"));
            }
        }
    }
}
