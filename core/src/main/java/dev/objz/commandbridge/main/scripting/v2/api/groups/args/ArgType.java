package dev.objz.commandbridge.main.scripting.v3.api.groups.args;

import java.util.Set;

import dev.objz.commandbridge.main.scripting.v3.api.ProblemSink;
import dev.objz.commandbridge.main.scripting.v3.api.groups.defaults.TargetKind;

public enum ArgType {
	BOOLEAN(Set.of(TargetKind.Type.BACKEND, TargetKind.Type.VELOCITY)),
	NUMBER(Set.of(TargetKind.Type.BACKEND, TargetKind.Type.VELOCITY)),
	WORD(Set.of(TargetKind.Type.BACKEND, TargetKind.Type.VELOCITY)),
	TEXT(Set.of(TargetKind.Type.BACKEND, TargetKind.Type.VELOCITY)),

	PLAYER(Set.of(TargetKind.Type.BACKEND, TargetKind.Type.VELOCITY)),
	ENTITY(Set.of(TargetKind.Type.BACKEND)),

	WORLD(Set.of(TargetKind.Type.BACKEND, TargetKind.Type.VELOCITY)),
	LOCATION(Set.of(TargetKind.Type.BACKEND)),

	UUID(Set.of(TargetKind.Type.BACKEND, TargetKind.Type.VELOCITY)),
	CHOICE(Set.of(TargetKind.Type.BACKEND, TargetKind.Type.VELOCITY)),
	RANGE(Set.of(TargetKind.Type.BACKEND, TargetKind.Type.VELOCITY)),

	ITEM_STACK(Set.of(TargetKind.Type.BACKEND)),
	ENCHANTMENT(Set.of(TargetKind.Type.BACKEND)),
	SOUND(Set.of(TargetKind.Type.BACKEND)),
	BIOME(Set.of(TargetKind.Type.BACKEND));

	private final Set<TargetKind.Type> defaultAllowedRegisters;

	ArgType(Set<TargetKind.Type> defaultAllowedRegisters) {
		this.defaultAllowedRegisters = defaultAllowedRegisters;
	}

	public Set<TargetKind.Type> getDefaultAllowedRegisters() {
		return defaultAllowedRegisters;
	}

	public Object coerceValue(Object raw, ProblemSink problems, String path) {
		return switch (this) {
			case BOOLEAN -> coerceBoolean(raw, problems, path);
			case NUMBER -> coerceNumber(raw, problems, path);
			case WORD, TEXT -> coerceString(raw);
			case UUID -> coerceUUID(raw, problems, path);
			case PLAYER, ENTITY, WORLD -> coerceString(raw);
			case LOCATION -> coerceString(raw);
			case ITEM_STACK, ENCHANTMENT, SOUND, BIOME -> coerceString(raw);
			case CHOICE, RANGE -> throw new UnsupportedOperationException(
					"Special types must handle coercion themselves");
		};
	}

	private Object coerceBoolean(Object raw, ProblemSink problems, String fieldPath) {
		return switch (raw) {
			case Boolean b -> b;
			case String s -> Boolean.parseBoolean(s);
			case null -> false;
			default -> {
				problems.error(fieldPath, "Cannot coerce " + raw.getClass() + " to boolean");
				yield false;
			}
		};
	}

	private Object coerceNumber(Object raw, ProblemSink problems, String fieldPath) {
		return switch (raw) {
			case Number n -> n.doubleValue();
			case String s -> {
				try {
					yield Double.parseDouble(s);
				} catch (NumberFormatException e) {
					problems.error(fieldPath, "Cannot parse '" + s + "' as number");
					yield 0.0;
				}
			}
			case null -> 0.0;
			default -> {
				problems.error(fieldPath, "Cannot coerce " + raw.getClass() + " to number");
				yield 0.0;
			}
		};
	}

	private Object coerceString(Object raw) {
		return switch (raw) {
			case String s -> s;
			case null -> "";
			default -> raw.toString();
		};
	}

	private Object coerceUUID(Object raw, ProblemSink problems, String fieldPath) {
		return switch (raw) {
			case String s -> {
				try {
					yield java.util.UUID.fromString(s);
				} catch (IllegalArgumentException e) {
					problems.error(fieldPath, "Invalid UUID format: " + s);
					yield null;
				}
			}
			case java.util.UUID u -> u;
			case null -> null;
			default -> {
				problems.error(fieldPath, "Cannot coerce " + raw.getClass() + " to UUID");
				yield null;
			}
		};
	}
}
