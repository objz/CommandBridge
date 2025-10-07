package dev.objz.commandbridge.scripting.model;

import java.util.List;

import dev.objz.commandbridge.scripting.anno.Default;
import dev.objz.commandbridge.scripting.anno.Max;
import dev.objz.commandbridge.scripting.anno.Min;
import dev.objz.commandbridge.scripting.anno.ModelRoot;
import dev.objz.commandbridge.scripting.anno.Pattern;
import dev.objz.commandbridge.scripting.anno.Required;
import dev.objz.commandbridge.scripting.model.records.mapping.ArgMapping;
import dev.objz.commandbridge.scripting.model.records.mapping.CmdMapping;
import dev.objz.commandbridge.scripting.model.records.mapping.IdMapping;

@ModelRoot("script")
public record Script(
		@Min(1) @Max(2) @Required int version,

		@Required @Pattern(regex = "^[a-z][a-z0-9-]{2,32}$") String name,

		@Default("true") boolean enabled,

		String description,

		List<String> aliases,

		Permissions permissions,

		List<IdMapping> register,

		Defaults defaults,

		List<ArgMapping> args,

		List<CmdMapping> commands

) {
}
