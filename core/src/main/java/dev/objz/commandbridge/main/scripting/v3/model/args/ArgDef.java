package dev.objz.commandbridge.main.scripting.v3.model.args;

import dev.objz.commandbridge.main.scripting.v3.enums.ArgType;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public sealed interface ArgDef permits SimpleArgDef, ChoiceArgDef {
	String name();

	Boolean required(); // default false

	ArgType type();
}
