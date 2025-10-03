package dev.objz.commandbridge.scripting.model;

import dev.objz.commandbridge.scripting.anno.Default;
import dev.objz.commandbridge.scripting.anno.Model;

@Model("permissions")
public record Permissions(
		@Default("true") boolean enabled,
		@Default("false") boolean silent

) {
}
