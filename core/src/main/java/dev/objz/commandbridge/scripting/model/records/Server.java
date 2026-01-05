package dev.objz.commandbridge.scripting.model.records;

import java.time.Duration;

import dev.objz.commandbridge.scripting.anno.Default;
import dev.objz.commandbridge.scripting.anno.Min;
import dev.objz.commandbridge.scripting.anno.Model;
import dev.objz.commandbridge.scripting.anno.YmlKey;

@Model("server")
public record Server(
		@YmlKey("target-required") @Default("false") boolean targetRequired,
		@YmlKey("schedule-online") @Default("false") boolean scheduleOnline,
		@Min(1) @Default("5s") Duration timeout) {
}
