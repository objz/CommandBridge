package dev.objz.commandbridge.velocity.dispatch;

import com.velocitypowered.api.command.CommandSource;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.logging.Summary;
import dev.objz.commandbridge.net.OutNode;
import dev.objz.commandbridge.net.payloads.feedback.Feedback;
import dev.objz.commandbridge.net.proto.MessageType;
import dev.objz.commandbridge.scripting.model.Script;
import dev.objz.commandbridge.scripting.model.enums.Location;
import dev.objz.commandbridge.api.channel.command.RunAs;
import dev.objz.commandbridge.scripting.model.records.mapping.CmdMapping;
import dev.objz.commandbridge.scripting.model.records.mapping.IdMapping;
import dev.objz.commandbridge.velocity.dispatch.exec.LocalDispatcher;
import dev.objz.commandbridge.velocity.dispatch.model.ExecutionContext;
import dev.objz.commandbridge.velocity.net.out.ctx.ExecuteCommandContext;
import dev.objz.commandbridge.velocity.net.session.ClientSession;
import dev.objz.commandbridge.velocity.net.session.SessionHub;
import dev.objz.commandbridge.velocity.util.PlayerTracker;
import dev.objz.commandbridge.util.MM;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class CommandDispatcher {

    private final SessionHub sessions;
    private final OutNode outNode;
    private final LocalDispatcher velocityExecutor;
    private final PlayerTracker playerTracker;

    public CommandDispatcher(SessionHub sessions, OutNode outNode, LocalDispatcher velocityExecutor,
                             PlayerTracker playerTracker) {
        this.sessions = sessions;
        this.outNode = outNode;
        this.velocityExecutor = velocityExecutor;
        this.playerTracker = playerTracker;
    }

    public void dispatchCommand(ExecutionContext ctx, CmdMapping cmd) {
        var targets = Optional.ofNullable(cmd.execute()).orElse(List.of());

        if (targets.isEmpty()) {
            Log.warn("Command '{}' has no execution targets defined", cmd.command());
            notifyExecutionError(ctx.source(), cmd.command(), "No execution targets configured");
            return;
        }

        for (IdMapping target : targets) {
            dispatchToTarget(ctx, cmd, target);
        }
    }

    private void dispatchToTarget(ExecutionContext ctx, CmdMapping cmd, IdMapping target) {
        String targetId = target.id();
        Location targetLoc = target.location();

        UUID playerUuid = ctx.getPlayerUuid();

        if (cmd.server() != null && cmd.server().targetRequired() && playerUuid != null) {
            boolean playerOnTarget = playerTracker.isPlayerOnTarget(
                    playerUuid, targetId, targetLoc, velocityExecutor.getLocalServerId());
            if (!playerOnTarget) {
                Log.warn("target-required: player '{}' is not on {} '{}', skipping",
                        playerUuid, targetLoc, targetId);
                return;
            }
        }

        if (targetLoc == Location.VELOCITY && velocityExecutor.isLocal(targetId)) {
            executeLocally(ctx, cmd);
            return;
        }

        Optional<ClientSession> sessionOpt = sessions.findSession(targetId, targetLoc);

        if (sessionOpt.isPresent()) {
            dispatchCommand(sessionOpt.get(), ctx, cmd);
        } else {
            Log.warn("Target '{}' ({}) not found or not connected", targetId, targetLoc);
            notifyExecutionError(ctx.source(), cmd.command(),
                    targetLoc + " server '" + targetId + "' is not connected");

            Feedback feedback = new Feedback(1, 0, 1, List.of(),
                    List.of(targetLoc + " not connected: " + targetId));
            Summary.feedbackSummary("Execution Failed", feedback, targetId);
        }
    }

    private void executeLocally(ExecutionContext ctx, CmdMapping cmd) {
        var playerUuid = ctx.getPlayerUuid();
        var runAs = Optional.ofNullable(cmd.runAs()).orElse(RunAs.CONSOLE);

        velocityExecutor.execute(cmd.command(), runAs, playerUuid, ctx.source())
                .thenAccept(success -> {
                    if (!success) {
                        Log.warn("Local Velocity command '{}' execution failed", cmd.command());
                        notifyExecutionError(ctx.source(), cmd.command(),
                                "Command execution failed");
                    }
                });
    }

    private void dispatchCommand(ClientSession session, ExecutionContext ctx, CmdMapping cmd) {
        var uuid = ctx.getPlayerUuid();
        var runAs = Optional.ofNullable(cmd.runAs()).orElse(RunAs.CONSOLE);

        Set<String> grantedPermissions = null;
        if (runAs == RunAs.OPERATOR && ctx.script() != null) {
            grantedPermissions = buildOPPermissions(ctx.script(), cmd);
        }

        Log.debug("Dispatching command to '{}' ({}): {}", session.id(), session.location(), cmd.command());

        outNode.send(MessageType.EXECUTE_COMMAND,
                new ExecuteCommandContext(session, cmd.command(), runAs, uuid, grantedPermissions));
    }

    private Set<String> buildOPPermissions(Script script, CmdMapping currentCmd) {
        Set<String> permissions = new HashSet<>();
        permissions.add("commandbridge.command." + script.name());

        String cmd = currentCmd.command();
        if (cmd == null || cmd.isBlank()) {
            return permissions;
        }

        String[] tokens = cmd.trim().split("\\s+");
        String base = tokens[0].replace("/", "");
        if (base.isEmpty()) {
            return permissions;
        }

        permissions.add(base);
        permissions.add(base + ".*");
        return permissions;
    }

    private void notifyExecutionError(CommandSource source, String command, String errorMessage) {
        if (source == null)
            return;
        String safe = errorMessage != null ? errorMessage.replace("<", "\\<") : "unknown";
        source.sendMessage(MM.parse("<red>⚠</red> <gray>Execution failed: " + safe + "</gray>"));
    }
}
