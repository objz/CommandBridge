package dev.objz.commandbridge.main.scripting.v3.model.args;

import dev.objz.commandbridge.main.scripting.v3.enums.ArgType;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;

public final class ArgDefSerializer implements TypeSerializer<ArgDef> {

	@Override
	public ArgDef deserialize(Type type, ConfigurationNode node) throws SerializationException {
		if (node == null || node.virtual())
			return null;

		String raw = node.node("type").getString();
		ArgType argType = null;
		if (raw != null && !raw.isBlank()) {
			try {
				argType = ArgType.valueOf(raw.trim().toUpperCase());
			} catch (IllegalArgumentException ignored) {
				// fall back to SimpleArgDef
			}
		}

		Class<? extends ArgDef> impl = (argType == ArgType.CHOICE) ? ChoiceArgDef.class
						: SimpleArgDef.class;

		return node.get(impl);
	}

	@Override
	public void serialize(Type type, ArgDef obj, ConfigurationNode target) throws SerializationException {
		if (obj == null) {
			target.set(null);
			return;
		}
		if (obj instanceof ChoiceArgDef c) {
			target.set(ChoiceArgDef.class, c);
		} else if (obj instanceof SimpleArgDef s) {
			target.set(SimpleArgDef.class, s);
		} else {
			throw new SerializationException(target, type, "Unknown ArgDef impl: " + obj.getClass());
		}
	}
}
