package dev.objz.commandbridge.scripting.model.records.mapping;

import java.util.List;

import dev.objz.commandbridge.scripting.anno.Default;
import dev.objz.commandbridge.scripting.anno.Model;
import dev.objz.commandbridge.scripting.anno.Pattern;
import dev.objz.commandbridge.scripting.anno.Required;
import dev.objz.commandbridge.scripting.model.enums.ArgType;

@Model("args")
public record ArgMapping(
        @Required String name,
        @Default("false") boolean required,
        @Default("STRING") ArgType type,
        List<@Pattern(regex = "^[a-z0-9._+\\-]+$") String> suggestions) {
}
