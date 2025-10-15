package dev.objz.commandbridge.cmd;

import dev.jorel.commandapi.arguments.Argument;
import dev.objz.commandbridge.scripting.model.records.mapping.ArgMapping;

public interface ArgumentMapper {
	Argument<?> map(ArgMapping argMapping);
}
