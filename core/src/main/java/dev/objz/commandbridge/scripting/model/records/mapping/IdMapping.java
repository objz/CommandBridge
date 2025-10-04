package dev.objz.commandbridge.scripting.model.records.mapping;

import dev.objz.commandbridge.scripting.anno.Model;
import dev.objz.commandbridge.scripting.anno.Required;
import dev.objz.commandbridge.scripting.model.enums.Location;

@Model("id")
public record IdMapping(
		@Required String id,
		@Required Location location

) {
}
