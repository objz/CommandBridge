package dev.objz.commandbridge.main.proto.cmd;


import java.util.List;

import dev.objz.commandbridge.main.scripting.v3.model.domain.Script.ArgType;



public record CommandArg(
		String name,
		int index,
		boolean required,
		ArgType type,
		Long min,
		Long max,
		List<String> choices) {
}
