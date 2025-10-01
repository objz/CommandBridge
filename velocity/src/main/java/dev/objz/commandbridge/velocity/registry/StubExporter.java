package dev.objz.commandbridge.velocity.registry;

import dev.objz.commandbridge.main.proto.cmd.CommandArg;
import dev.objz.commandbridge.main.proto.cmd.CommandStub;
import dev.objz.commandbridge.main.scripting.v3.model.domain.Script;

import java.util.ArrayList;
import java.util.List;

/** Convert resolved v3 Scripts -> Command stubs for registration. */
public final class StubExporter {
	private StubExporter() {
	}

	public static List<CommandStub> export(List<Script> scripts) {
		List<CommandStub> out = new ArrayList<>();
		if (scripts == null || scripts.isEmpty())
			return out;

		for (Script s : scripts) {
			var argsOut = new ArrayList<CommandArg>();
			var defs = s.args();
			if (defs != null && !defs.isEmpty()) {
				for (int i = 0; i < defs.size(); i++) {
					var d = defs.get(i);
					// min/max/choices not present in current resolved model; keep nulls
					argsOut.add(new CommandArg(d.name(), i, d.required(), d.type(), null, null,
							null));
				}
			}
			out.add(new CommandStub(s.name(), s.aliases(), s.description(), argsOut));
		}
		return out;
	}
}
