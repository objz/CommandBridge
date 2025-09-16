package dev.objz.commandbridge.main.proto.cmd;

import java.util.List;

public record RegisterCommandsResultPayload(
		int requested,
		int registered,
		int failed,
		List<String> warnings,
		List<String> errors) {
}
