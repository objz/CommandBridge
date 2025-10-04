package dev.objz.commandbridge.scripting.model;

import java.util.List;

import dev.objz.commandbridge.scripting.anno.Default;
import dev.objz.commandbridge.scripting.anno.ModelRoot;
import dev.objz.commandbridge.scripting.anno.Required;
import dev.objz.commandbridge.scripting.model.records.mapping.ArgMapping;
import dev.objz.commandbridge.scripting.model.records.mapping.CmdMapping;
import dev.objz.commandbridge.scripting.model.records.mapping.IdMapping;

@ModelRoot("script")
public record Script(
		@Required int version,

		@Required String name,

		@Default("true") boolean enabled,

		String description,

		@Default("[]")

		List<String> aliases,

		Permissions permissions,

		List<IdMapping> register,

		Defaults defaults,

		List<ArgMapping> args,

		List<CmdMapping> commands

) {
}
