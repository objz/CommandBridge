package dev.objz.commandbridge.main.scripting.v3.usage;

import dev.objz.commandbridge.main.scripting.v3.effective.EffectiveModels;

import java.util.ArrayList;

public final class UsageBuilder {
	private UsageBuilder() {
	}

	public static String build(EffectiveModels.Script s) {
		var args = s.args().spec();
		if (args == null || args.isEmpty())
			return "/" + s.name();
		var parts = new ArrayList<String>(args.size());
		for (var a : args)
			parts.add(a.required() ? "<" + a.name() + ">" : "[" + a.name() + "]");
		return "/" + s.name() + " " + String.join(" ", parts);
	}
}
