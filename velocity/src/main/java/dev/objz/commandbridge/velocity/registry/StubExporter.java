package dev.objz.commandbridge.velocity.registry;

import dev.objz.commandbridge.main.proto.cmd.CommandArg;
import dev.objz.commandbridge.main.proto.cmd.CommandStub;
import dev.objz.commandbridge.main.scripting.v3.effective.EffectiveModels;

import java.util.ArrayList;
import java.util.List;

public final class StubExporter {
	private StubExporter() {
	}

	public static List<CommandStub> export(List<EffectiveModels.Script> scripts) {
		var out = new ArrayList<CommandStub>();
		for (var s : scripts) {
			var effArgs = (s.args() == null || s.args().spec() == null) ? List.<CommandArg>of()
					: s.args().spec().stream()
							.map(a -> new CommandArg(
									a.name(), a.index(), a.required(), a.type(),
									a.min(), a.max(), a.choices()))
							.toList();
			out.add(new CommandStub(s.name(), s.aliases(), s.description(), effArgs));
		}
		return out;
	}
}
