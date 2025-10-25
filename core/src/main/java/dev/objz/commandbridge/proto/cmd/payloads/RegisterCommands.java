package dev.objz.commandbridge.proto.cmd.payloads;

import java.util.List;

import dev.objz.commandbridge.proto.cmd.CommandStub;

public record RegisterCommands(
		List<CommandStub> commands) {
}
