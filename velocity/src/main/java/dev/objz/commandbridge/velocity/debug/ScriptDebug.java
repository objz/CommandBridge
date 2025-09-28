package dev.objz.commandbridge.velocity.debug;

import dev.objz.commandbridge.main.logging.Log;
import dev.objz.commandbridge.main.scripting.v3.effective.EffectiveModels;

public final class ScriptDebug {
	private ScriptDebug() {
	}

	public static void dump(EffectiveModels.Script s) {
		if (s == null) {
			Log.debug("[dbg] script: <null>");
			return;
		}

		Log.debug("[dbg] enabled         : {}", s.enabled());
		Log.debug("[dbg] --------------------------- SCRIPT ---------------------------");
		Log.debug("[dbg] version         : {}", s.version());
		Log.debug("[dbg] name            : {}", s.name());
		Log.debug("[dbg] description     : {}", nvl(s.description()));
		Log.debug("[dbg] aliases         : {}", String.join(", ", s.aliases()));

		var p = s.permissions();
		Log.debug("[dbg] permissions.enabled : {}", p.enabled());
		Log.debug("[dbg] permissions.silent  : {}", p.silent());

		var d = s.defaults();
		Log.debug("[dbg] defaults.delay   : {} ({} ms)", d.delay(), d.delay().toMillis());
		Log.debug("[dbg] defaults.timeout : {} ({} ms)", d.timeout(), d.timeout().toMillis());
		Log.debug("[dbg] defaults.cooldown: {} ({} ms)", d.cooldown(), d.cooldown().toMillis());

		var t = d.target();
		Log.debug("[dbg] target.run-as        : {}", t.runAs());
		Log.debug("[dbg] target.id            : {}", nvl(t.id()));
		Log.debug("[dbg] target.kind.register : {}", t.register());
		Log.debug("[dbg] target.kind.execute  : {}", t.execute());
		Log.debug("[dbg] target.targetRequired: {}", t.targetRequired());
		Log.debug("[dbg] target.scheduleOnline: {}", t.scheduleOnline());
		Log.debug("[dbg] target.timeout       : {} ({} ms)", t.scheduleTimeout(),
				t.scheduleTimeout().toMillis());
		Log.debug("[dbg] target.frequency     : {} ({} ms)", t.scheduleFrequency(),
				t.scheduleFrequency().toMillis());

		var a = s.args();
		if (a == null || a.specs() == null || a.specs().isEmpty()) {
			Log.debug("[dbg] args.spec        : <empty>");
		} else {
			for (int i = 0; i < a.specs().size(); i++) {
				var arg = a.specs().get(i);
				Log.debug("[dbg] args.spec[{}].name      : {}", i, arg.name());
				Log.debug("[dbg] args.spec[{}].index     : {}", i, arg.index());
				Log.debug("[dbg] args.spec[{}].required  : {}", i, arg.required());
				Log.debug("[dbg] args.spec[{}].type      : {}", i, arg.type());
				Log.debug("[dbg] args.spec[{}].min       : {}", i, arg.min());
				Log.debug("[dbg] args.spec[{}].max       : {}", i, arg.max());
				Log.debug("[dbg] args.spec[{}].choices   : {}", i,
						String.join(", ", arg.choices() == null ? java.util.List.of()
								: arg.choices()));
			}
		}

		var steps = s.steps();
		Log.debug("[dbg] steps.count      : {}", steps.size());
		for (int i = 0; i < steps.size(); i++) {
			var st = steps.get(i);
			Log.debug("[dbg] --- step[{}] ---------------------------------------------", i);
			Log.debug("[dbg] step[{}].command   : {}", i, st.command());
			Log.debug("[dbg] step[{}].delay     : {} ({} ms)", i, st.delay(), st.delay().toMillis());
			Log.debug("[dbg] step[{}].timeout   : {} ({} ms)", i, st.timeout(), st.timeout().toMillis());

			Log.debug("[dbg] step[{}].run-as    : {}", i, st.runAs());
			Log.debug("[dbg] step[{}].id        : {}", i, nvl(st.id()));
			Log.debug("[dbg] step[{}].register  : {}", i, st.register());
			Log.debug("[dbg] step[{}].execute   : {}", i, st.execute());
			Log.debug("[dbg] step[{}].tgtRequired: {}", i, st.targetRequired());
			Log.debug("[dbg] step[{}].schedOnline: {}", i, st.scheduleOnline());
			Log.debug("[dbg] step[{}].timeout   : {} ({} ms)", i, st.timeout(),
					st.timeout() == null ? 0 : st.timeout().toMillis());
			Log.debug("[dbg] step[{}].frequency : {} ({} ms)", i, st.frequency(),
					st.frequency() == null ? 0 : st.frequency().toMillis());
		}

		Log.debug("[dbg] ----------------------------------------------------------------");
	}

	private static String nvl(String s) {
		return (s == null || s.isBlank()) ? "<null>" : s;
	}
}
