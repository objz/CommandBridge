package dev.objz.commandbridge.velocity.registry;

import dev.objz.commandbridge.main.proto.cmd.CommandStub;
import dev.objz.commandbridge.main.scripting.v3.effective.EffectiveModels;
import dev.objz.commandbridge.main.scripting.v3.usage.UsageBuilder;

import java.util.ArrayList;
import java.util.List;

public final class StubExporter {
	private StubExporter() {
	}

	public static List<CommandStub> export(List<EffectiveModels.Script> scripts) {
		var out = new ArrayList<CommandStub>();
		for (var s : scripts)
			out.add(new CommandStub(s.name(), s.aliases(), s.description(), UsageBuilder.build(s)));
		return out;
	}
}
