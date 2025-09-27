package dev.objz.commandbridge.main.scripting.v3.resolver;

import dev.objz.commandbridge.main.logging.Log;
import dev.objz.commandbridge.main.scripting.v3.effective.EffectiveModels;
import dev.objz.commandbridge.main.scripting.v3.enums.ArgType;
import dev.objz.commandbridge.main.scripting.v3.model.args.*;

import java.util.*;

public final class ArgsResolver {
	public EffectiveModels.Args toEffective(List<ArgDef> defs) {
		if (defs == null || defs.isEmpty())
			return new EffectiveModels.Args(List.of());
		List<EffectiveModels.Arg> out = new ArrayList<>(defs.size());
		for (int i = 0; i < defs.size(); i++) {
			ArgDef d = defs.get(i);
			boolean required = d.required() != null && d.required();
			ArgType type = d.type();
			Long min = null, max = null;
			java.util.List<String> choices = java.util.List.of();

			if (d instanceof ChoiceArgDef c) {
				choices = c.choices() == null ? java.util.List.of()
						: java.util.List.copyOf(c.choices());
			}

			out.add(new EffectiveModels.Arg(d.name(), i, required, type, min, max, choices));

			if (d instanceof SimpleArgDef s && (s.type() == ArgType.CHOICE || s.type() == ArgType.RANGE)) {
				Log.warn("Arg '{}' declared as SIMPLE but type is {}. Prefer dedicated def class.",
						s.name(), s.type());
			}
		}
		for (int i = 0; i < out.size(); i++)
			for (int j = i + 1; j < out.size(); j++) {
				if (equivalent(out.get(i), out.get(j))) {
					Log.warn("Args '{}' and '{}' are identical; consider removing one",
							out.get(i).name(), out.get(j).name());
				}
			}
		return new EffectiveModels.Args(java.util.List.copyOf(out));
	}

	private static boolean equivalent(EffectiveModels.Arg a, EffectiveModels.Arg b) {
		return a.type() == b.type() && a.required() == b.required() &&
				Objects.equals(a.min(), b.min()) && Objects.equals(a.max(), b.max()) &&
				Objects.equals(a.choices(), b.choices());
	}
}
