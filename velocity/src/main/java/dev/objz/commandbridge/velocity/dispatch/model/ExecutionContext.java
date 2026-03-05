package dev.objz.commandbridge.velocity.dispatch.model;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import dev.objz.commandbridge.net.payloads.cmd.InvokedCommand;
import dev.objz.commandbridge.scripting.model.Script;
import dev.objz.commandbridge.scripting.model.records.mapping.CmdMapping;
import dev.objz.commandbridge.velocity.net.session.ClientSession;

import java.util.Map;
import java.util.UUID;

public record ExecutionContext(
        InvokedCommand invoked,
        ClientSession session,
        CommandSource source,
        UUID playerUuid,
        Script script,
        Map<String, Object> arguments,
        CmdMapping currentCommand,
        int commandIndex) {

    public UUID getPlayerUuid() {
        if (playerUuid != null) return playerUuid;
        return source instanceof Player p ? p.getUniqueId() : null;
    }

    public ExecutionContext withScript(Script script) {
        return new ExecutionContext(invoked, session, source, playerUuid, script, arguments, currentCommand, commandIndex);
    }

    public ExecutionContext withArguments(Map<String, Object> args) {
        return new ExecutionContext(invoked, session, source, playerUuid, script, args, currentCommand, commandIndex);
    }

    public ExecutionContext nextCommand(CmdMapping cmd, int index) {
        return new ExecutionContext(invoked, session, source, playerUuid, script, arguments, cmd, index);
    }

    public ExecutionContext withPlayerUuid(UUID uuid) {
        return new ExecutionContext(invoked, session, source, uuid, script, arguments, currentCommand, commandIndex);
    }
}
