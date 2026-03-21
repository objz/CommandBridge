package dev.objz.commandbridge.scripting.model.records.mapping;

import java.time.Duration;
import java.util.List;

import dev.objz.commandbridge.scripting.anno.Merge;
import dev.objz.commandbridge.scripting.anno.Min;
import dev.objz.commandbridge.scripting.anno.Model;
import dev.objz.commandbridge.scripting.anno.Required;
import dev.objz.commandbridge.scripting.anno.Resolvable;
import dev.objz.commandbridge.scripting.anno.YmlKey;
import dev.objz.commandbridge.api.channel.command.RunAs;
import dev.objz.commandbridge.scripting.model.records.Server;

@Model("commands")
public record CmdMapping(
        @Required @Resolvable String command,

        @Merge @YmlKey("run-as") RunAs runAs,

        @Merge List<IdMapping> execute,

        @Merge Server server,

        @Merge @Min(0) Duration delay,

        @Merge @Min(0) Duration cooldown) {
}
