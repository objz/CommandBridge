package dev.objz.commandbridge.main.scripting.v3.api.groups.args;

import java.util.Set;

import dev.objz.commandbridge.main.scripting.v3.api.ProblemSink;
import dev.objz.commandbridge.main.scripting.v3.api.groups.Arg;
import dev.objz.commandbridge.main.scripting.v3.api.groups.defaults.TargetKind;

public record RangeArg(
		String name,
		boolean required,
		Set<TargetKind.Type> allowedRegisters,
		double min,
		double max) implements Arg {

	@Override
	public ArgType type() {
		return ArgType.RANGE;
	}

	@Override
	public void validate(ProblemSink problems) {
		Arg.super.validate(problems);
		if (min >= max) {
			problems.error("args[" + name() + "].range", "Min value must be less than max value");
		}
	}

	@Override
	public Object coerceRuntimeValue(Object raw, ProblemSink problems) {
		double value = switch (raw) {
			case Number n -> n.doubleValue();
			case String s -> {
				try {
					yield Double.parseDouble(s);
				} catch (NumberFormatException e) {
					problems.error("args[" + name() + "].value",
							"Cannot parse '" + s + "' as number");
					yield min; 
				}
			}
			case null -> min;
			default -> {
				problems.error("args[" + name() + "].value",
						"Cannot coerce " + raw.getClass() + " to number");
				yield min;
			}
		};

		if (value < min || value > max) {
			problems.error("args[" + name() + "].value",
					"Value " + value + " is outside range [" + min + ", " + max + "]");
			return Math.max(min, Math.min(max, value)); 
		}

		return value;
	}

	public static RangeArg withDefaults(String name, boolean required, double min, double max) {
		return new RangeArg(name, required, ArgType.RANGE.getDefaultAllowedRegisters(), min, max);
	}

	public static RangeArg backendOnly(String name, boolean required, double min, double max) {
		return new RangeArg(name, required, Set.of(TargetKind.Type.BACKEND), min, max);
	}

	public static RangeArg both(String name, boolean required, double min, double max) {
		return new RangeArg(name, required, Set.of(TargetKind.Type.BACKEND, TargetKind.Type.VELOCITY), min,
				max);
	}
}
