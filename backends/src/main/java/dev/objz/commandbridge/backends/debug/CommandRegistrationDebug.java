package dev.objz.commandbridge.backends.debug;

import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.proto.cmd.CommandStub;

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
