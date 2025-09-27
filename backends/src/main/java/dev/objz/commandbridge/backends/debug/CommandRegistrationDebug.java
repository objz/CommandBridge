package dev.objz.commandbridge.backends.debug;

import dev.objz.commandbridge.main.logging.Log;
import dev.objz.commandbridge.main.proto.cmd.CommandArg;
import dev.objz.commandbridge.main.proto.cmd.CommandStub;

import java.util.List;

public final class CommandRegistrationDebug {
	private CommandRegistrationDebug() {
	}

	public static void dumpStubs(List<CommandStub> stubs) {
		if (stubs == null) {
			Log.debug("[dbg] stubs: <null>");
			return;
		}
		Log.debug("[dbg] ---------------------- COMMAND STUBS -----------------------");
		Log.debug("[dbg] stubs.count      : {}", stubs.size());
		for (int i = 0; i < stubs.size(); i++) {
			CommandStub s = stubs.get(i);
			Log.debug("[dbg] --- stub[{}] ------------------------------------------------", i);
			if (s == null) {
				Log.debug("[dbg] stub[{}] : <null>", i);
				continue;
			}
			Log.debug("[dbg] stub[{}].name        : {}", i, nvl(s.name()));
			Log.debug("[dbg] stub[{}].aliases     : {}", i, join(s.aliases()));
			Log.debug("[dbg] stub[{}].description : {}", i, nvl(s.description()));

			var args = s.args();
			if (args == null || args.isEmpty()) {
				Log.debug("[dbg] stub[{}].args        : <empty>", i);
			} else {
				for (int a = 0; a < args.size(); a++) {
					CommandArg ar = args.get(a);
					Log.debug("[dbg] stub[{}].args[{}].name      : {}", i, a, ar.name());
					Log.debug("[dbg] stub[{}].args[{}].index     : {}", i, a, ar.index());
					Log.debug("[dbg] stub[{}].args[{}].required  : {}", i, a, ar.required());
					Log.debug("[dbg] stub[{}].args[{}].type      : {}", i, a, ar.type());
					Log.debug("[dbg] stub[{}].args[{}].min       : {}", i, a, ar.min());
					Log.debug("[dbg] stub[{}].args[{}].max       : {}", i, a, ar.max());
					Log.debug("[dbg] stub[{}].args[{}].choices   : {}", i, a,
							(ar.choices() == null || ar.choices().isEmpty())
									? "<empty>"
									: String.join(", ", ar.choices()));
				}
			}
		}
		Log.debug("[dbg] ----------------------------------------------------------------");
	}

	private static String join(List<String> list) {
		return (list == null || list.isEmpty()) ? "<empty>" : String.join(", ", list);
	}

	private static String nvl(String s) {
		return (s == null || s.isBlank()) ? "<null>" : s;
	}
}
