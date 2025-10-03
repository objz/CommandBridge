package dev.objz.commandbridge.velocity.registry;

import dev.objz.commandbridge.proto.cmd.CommandStub;
import dev.objz.commandbridge.scripting.model.Script;

import java.util.ArrayList;
import java.util.List;

public final class StubExporter {
	private StubExporter() {
	}

	public static List<CommandStub> export(List<Script> scripts) {
		List<CommandStub> out = new ArrayList<>();
		if (scripts == null || scripts.isEmpty())
			return out;

		for (Script s : scripts) {
			out.add(new CommandStub(s.name(), s.aliases(), s.description()));
		}
		return out;
	}
}
