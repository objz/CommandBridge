package dev.objz.commandbridge.net.payloads.cmd;

import java.util.Set;
import java.util.UUID;
import dev.objz.commandbridge.api.channel.command.RunAs;

public record ExecuteCommand(
        String command,
        RunAs runAs,
        UUID uuid,
        Set<String> grantedPermissions) {

}
