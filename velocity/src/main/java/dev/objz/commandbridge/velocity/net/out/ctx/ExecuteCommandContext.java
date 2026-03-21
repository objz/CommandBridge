package dev.objz.commandbridge.velocity.net.out.ctx;

import dev.objz.commandbridge.api.channel.command.RunAs;
import dev.objz.commandbridge.velocity.net.session.ClientSession;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record ExecuteCommandContext(
        ClientSession session,
        String command,
        RunAs runAs,
        UUID uuid,
        Set<String> grantedPermissions) {

    public ExecuteCommandContext {
        Objects.requireNonNull(session);
        Objects.requireNonNull(command);
        Objects.requireNonNull(runAs);
    }
}
