package dev.objz.commandbridge.main.scripting.v3.enums;

public enum ArgType {
	// Basics
	BOOLEAN,
	NUMBER, // one numeric type (int/decimal)
	WORD, // single token
	TEXT, // supports quotes
	GREEDY_TEXT, // rest of the line

	// Players / entities
	PLAYER,
	MANY_PLAYERS,
	ENTITY,
	MANY_ENTITIES,

	// World / position
	WORLD,
	LOCATION,

	// Identifiers / choices
	UUID,
	LITERAL, // exact word
	CHOICE, // ["start","stop"]
	RANGE, // 0.1..10

	ITEM_STACK, // ItemStack
	TIME_TICKS, // 20, 40, 200
	ENCHANTMENT,
	SOUND,
	BIOME
}
