package dev.objz.commandbridge.net.payloads.cmd;

import java.util.Set;
import java.util.UUID;
import dev.objz.commandbridge.scripting.model.enums.RunAs;

public record ExecuteCommand(
		String command,
		RunAs runAs,
		UUID uuid,
		Set<String> grantedPermissions) {

}
