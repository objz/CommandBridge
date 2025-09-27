package dev.objz.commandbridge.main.proto.cmd;

import dev.objz.commandbridge.main.scripting.v3.enums.ArgType;

import java.util.List;

public record CommandArg(
		String name,
		int index,
		boolean required,
		ArgType type,
		Long min,
		Long max,
		List<String> choices) {
}
