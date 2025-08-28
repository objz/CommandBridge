package dev.objz.commandbridge.velocity.registry;

import dev.objz.commandbridge.main.proto.cmd.CommandStub;
import dev.objz.commandbridge.velocity.scripting.ScriptManager;
import dev.objz.commandbridge.main.scripting.Effective;

import java.util.ArrayList;
import java.util.List;

/**
 * A stub contains just enough metadata for
 * backends to register a dummy command
 */
public final class StubExporter {
	private StubExporter() {
	}

	public static List<CommandStub> export(ScriptManager mgr) {
		var out = new ArrayList<CommandStub>();
		for (Effective.Script s : mgr.enabled()) {
			String id = s.name();
			String primary = s.name();
			List<String> aliases = s.aliases() != null ? s.aliases() : List.of();
			String desc = s.description() != null ? s.description() : "";
			String usage = s.args() != null ? s.args().description() : "";
			out.add(new CommandStub(id, primary, aliases, desc, usage));
		}
		return out;
	}
}
