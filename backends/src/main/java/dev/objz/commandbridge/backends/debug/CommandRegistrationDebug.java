package dev.objz.commandbridge.backends.debug;

import dev.objz.commandbridge.backends.api.PlatformRegistry;
import dev.objz.commandbridge.main.logging.Log;
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
			Log.debug("[dbg] stub[{}].usage       : {}", i, nvl(s.usage()));
		}
		Log.debug("[dbg] ----------------------------------------------------------------");
	}

	public static void dumpResult(PlatformRegistry.RegistrationResult r) {
		if (r == null) {
			Log.debug("[dbg] result: <null>");
			return;
		}
		Log.debug("[dbg] ------------------- REGISTER RESULT -----------------------");
		Log.debug("[dbg] requested      : {}", r.requested());
		Log.debug("[dbg] registered     : {}", r.registered());
		Log.debug("[dbg] failed         : {}", r.failed());

		if (r.warnings() == null || r.warnings().isEmpty()) {
			Log.debug("[dbg] warnings.count : 0");
		} else {
			Log.debug("[dbg] warnings.count : {}", r.warnings().size());
			for (int i = 0; i < r.warnings().size(); i++) {
				Log.debug("[dbg] warnings[{}]   : {}", i, nvl(r.warnings().get(i)));
			}
		}

		if (r.errors() == null || r.errors().isEmpty()) {
			Log.debug("[dbg] errors.count   : 0");
		} else {
			Log.debug("[dbg] errors.count   : {}", r.errors().size());
			for (int i = 0; i < r.errors().size(); i++) {
				Log.debug("[dbg] errors[{}]     : {}", i, nvl(r.errors().get(i)));
			}
		}
		Log.debug("[dbg] ----------------------------------------------------------------");
	}

	private static String join(List<String> list) {
		if (list == null || list.isEmpty())
			return "<empty>";
		return String.join(", ", list);
	}

	private static String nvl(String s) {
		return (s == null || s.isBlank()) ? "<null>" : s;
	}
}
