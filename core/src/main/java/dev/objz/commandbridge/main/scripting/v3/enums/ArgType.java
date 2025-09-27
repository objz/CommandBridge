package dev.objz.commandbridge.main.scripting.v3.enums;

public enum ArgType {
	// Basics
	BOOLEAN,
	NUMBER, // one numeric type (int/decimal)
	WORD, // single token
	TEXT, // supports quotes

	// Players / entities
	PLAYER,
	ENTITY,

	// World / position
	WORLD,
	LOCATION,

	// Identifiers / choices
	UUID,
	CHOICE, // ["start","stop"]
	RANGE, // 0.1..10

	ITEM_STACK, // ItemStack
	ENCHANTMENT,
	SOUND,
	BIOME
}
