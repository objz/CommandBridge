package dev.objz.commandbridge.scripting.model;

import java.time.Duration;
import java.util.List;

import dev.objz.commandbridge.scripting.anno.Default;
import dev.objz.commandbridge.scripting.anno.Min;
import dev.objz.commandbridge.scripting.anno.Model;
import dev.objz.commandbridge.scripting.anno.YmlKey;
import dev.objz.commandbridge.api.channel.command.RunAs;
import dev.objz.commandbridge.scripting.model.records.Server;
import dev.objz.commandbridge.scripting.model.records.mapping.IdMapping;

@Model("defaults")
public record Defaults(
        @YmlKey("run-as") @Default("CONSOLE") RunAs runAs,

        List<IdMapping> execute,

        Server server,

        @Min(0) @Default("0s") Duration delay,

        @Min(0) @Default("0s") Duration cooldown

) {

}
