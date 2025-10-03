package dev.objz.commandbridge.scripting.model.records.mapping;

import dev.objz.commandbridge.scripting.anno.Default;
import dev.objz.commandbridge.scripting.anno.Model;
import dev.objz.commandbridge.scripting.anno.Required;
import dev.objz.commandbridge.scripting.model.enums.ArgType;

@Model("args")
public record ArgMapping(
		@Required String name,
		@Default("false") boolean required,
		@Default("STRING") ArgType type) {
}
