package dev.objz.commandbridge.scripting.model.enums;

import dev.objz.commandbridge.scripting.anno.Platform;
import dev.objz.commandbridge.scripting.platform.PlatformFeatureKeys;
import static dev.objz.commandbridge.scripting.model.enums.Location.*;

public enum ArgType {
	@Platform({ VELOCITY, BACKEND })
	STRING,
	@Platform({ VELOCITY, BACKEND })
	INTEGER,
	@Platform({ VELOCITY, BACKEND })
	BOOLEAN,
	@Platform({ VELOCITY, BACKEND })
	DOUBLE,
	@Platform({ VELOCITY, BACKEND })
	TEXT,
	@Platform({ VELOCITY, BACKEND })
	GREEDY_STRING,

	@Platform({ BACKEND })
	RANGE,
	// good idea: maybe add script ref to reference to other scripts?

	@Platform({ BACKEND })
	PLAYERS,
	@Platform({ BACKEND })
	ENTITIES,
	@Platform({ BACKEND })
	ENTITY_TYPE,

	@Platform({ BACKEND })
	WORLD,
	@Platform({ VELOCITY })
	SERVER,
	@Platform({ BACKEND })
	LOCATION,
	@Platform({ BACKEND })
	LOCATION_2D,
	@Platform({ BACKEND })
	ANGLE,
	@Platform({ BACKEND })
	ROTATION,

	@Platform({ BACKEND })
	ITEM_STACK,
	@Platform({ BACKEND })
	ENCHANTMENT,
	@Platform({ BACKEND })
	POTION_EFFECT,

	@Platform({ BACKEND })
	SOUND,
	@Platform({ BACKEND })
	BIOME,

	@Platform(value = { BACKEND }, optional = {
			@Platform.OptionalSupport(location = VELOCITY, feature = PlatformFeatureKeys.PACKET_EVENTS)
	})
	TIME,

}
