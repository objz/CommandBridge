package dev.objz.commandbridge.backends.platform.cmd;

import dev.jorel.commandapi.arguments.*;
import dev.objz.commandbridge.scripting.model.enums.ArgType;

/**
 * Maps ArgType enum values to CommandAPI Argument instances
 */
public final class ArgumentMapper {

	public Argument<?> map(String argName, ArgType type) {
		if (argName == null || argName.isBlank()) {
			throw new IllegalArgumentException("Argument name cannot be null or blank");
		}
		if (type == null) {
			throw new IllegalArgumentException("ArgType cannot be null");
		}

		return switch (type) {
			case STRING -> new StringArgument(argName);
			case INTEGER -> new IntegerArgument(argName);
			case BOOLEAN -> new BooleanArgument(argName);

			// Add more mappings as you extend ArgType
			// Examples based on the CommandAPI argument list you provided:

			// case FLOAT -> new FloatArgument(argName);
			// case DOUBLE -> new DoubleArgument(argName);
			// case LONG -> new LongArgument(argName);

			// case GREEDY_STRING -> new GreedyStringArgument(argName);
			// case TEXT -> new TextArgument(argName);

			// case PLAYER -> new PlayerArgument(argName);
			// case ENTITY -> new EntitySelectorArgument.OneEntity(argName);
			// case ENTITY_TYPE -> new EntityTypeArgument(argName);

			// case WORLD -> new WorldArgument(argName);
			// case LOCATION -> new LocationArgument(argName);
			// case LOCATION_2D -> new Location2DArgument(argName);

			// case ITEM_STACK -> new ItemStackArgument(argName);
			// case ENCHANTMENT -> new EnchantmentArgument(argName);
			// case POTION_EFFECT -> new PotionEffectArgument(argName);

			// case SOUND -> new SoundArgument(argName);
			// case BIOME -> new BiomeArgument(argName);

			// case ANGLE -> new AngleArgument(argName);
			// case ROTATION -> new RotationArgument(argName);

			// case UUID_ARG -> new UUIDArgument(argName);
			// case TIME -> new TimeArgument(argName);

			// Add more as needed...

			default -> throw new UnsupportedOperationException(
					"ArgType." + type + " is not yet mapped to a CommandAPI argument. " +
							"Please add the mapping in ArgumentMapper.map()");
		};
	}

	public boolean isSupported(ArgType type) {
		return switch (type) {
			case STRING, INTEGER, BOOLEAN -> true;
			default -> false;
		};
	}
}
