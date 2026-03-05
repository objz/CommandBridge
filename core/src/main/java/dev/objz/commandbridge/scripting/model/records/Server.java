package dev.objz.commandbridge.scripting.model.records;

import dev.objz.commandbridge.scripting.anno.Default;
import dev.objz.commandbridge.scripting.anno.Model;
import dev.objz.commandbridge.scripting.anno.YmlKey;

@Model("server")
public record Server(
        @YmlKey("target-required") @Default("false") boolean targetRequired,
        @YmlKey("schedule-online") @Default("false") boolean scheduleOnline,
        @YmlKey("player-arg") String playerArg) {
}
