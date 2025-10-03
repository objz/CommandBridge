package dev.objz.commandbridge.scripting.model.records.mapping;

import java.time.Duration;
import java.util.List;

import dev.objz.commandbridge.scripting.anno.Model;
import dev.objz.commandbridge.scripting.anno.Required;
import dev.objz.commandbridge.scripting.anno.YmlKey;
import dev.objz.commandbridge.scripting.model.enums.RunAs;
import dev.objz.commandbridge.scripting.model.records.Server;

@Model("commands")
public record CmdMapping(
		@Required String command,

		@YmlKey("run-as") RunAs runAs,

		List<IdMapping> execute,

		Server server,

		Duration delay,

		Duration cooldown) {
}
