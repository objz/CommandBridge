package dev.objz.commandbridge.velocity.debug;

import dev.objz.commandbridge.main.logging.Log;
import dev.objz.commandbridge.main.scripting.Effective;

public final class ScriptDebug {
	private ScriptDebug() {}

	public static void dump(Effective.Script s) {
		if (s == null) {
			Log.debug("[dbg] script: <null>");
			return;
		}

		Log.debug("[dbg] enabled         : {}", s.enabled());
		Log.debug("[dbg] --------------------------- SCRIPT ---------------------------");
		Log.debug("[dbg] version         : {}", s.version());
		Log.debug("[dbg] kind            : {}", s.kind());
		Log.debug("[dbg] name            : {}", s.name());
		Log.debug("[dbg] description     : {}", s.description());
		Log.debug("[dbg] aliases         : {}", String.join(", ", s.aliases()));

		var p = s.permissions();
		Log.debug("[dbg] permissions.enabled : {}", p.enabled());
		Log.debug("[dbg] permissions.silent  : {}", p.silent());

		Log.debug("[dbg] run-as          : {}", s.runAs());
		var t = s.target();
		Log.debug("[dbg] target.mode         : {}", t.mode());
		Log.debug("[dbg] target.fixed        : {}", nvl(t.fixed()));
		Log.debug("[dbg] target.arg-index    : {}", t.argIndex());
		Log.debug("[dbg] target.require-online     : {}", t.requireOnline());
		Log.debug("[dbg] target.require-on-server  : {}", t.requireOnServer());

		var d = s.defaults();
		Log.debug("[dbg] defaults.delay   : {} ({} ms)", d.delay(), d.delay().toMillis());
		Log.debug("[dbg] defaults.timeout : {} ({} ms)", d.timeout(), d.timeout().toMillis());
		Log.debug("[dbg] defaults.cooldown: {} ({} ms)", d.cooldown(), d.cooldown().toMillis());
		if (d.rateLimit() == null) {
			Log.debug("[dbg] defaults.rate-limit : <none>");
		} else {
			var rl = d.rateLimit();
			Log.debug("[dbg] defaults.rate-limit.count  : {}", rl.count());
			Log.debug("[dbg] defaults.rate-limit.window : {} ({} ms)", rl.window(), rl.window().toMillis());
			Log.debug("[dbg] defaults.rate-limit.scope  : {}", rl.scope());
		}

		var a = s.args();
		Log.debug("[dbg] args.description : {}", a.description());
		if (a.spec().isEmpty()) {
			Log.debug("[dbg] args.spec        : <empty>");
		} else {
			for (int i = 0; i < a.spec().size(); i++) {
				var arg = a.spec().get(i);
				Log.debug("[dbg] args.spec[{}].name      : {}", i, arg.name());
				Log.debug("[dbg] args.spec[{}].index     : {}", i, arg.index());
				Log.debug("[dbg] args.spec[{}].required  : {}", i, arg.required());
				Log.debug("[dbg] args.spec[{}].type      : {}", i, arg.type());
				Log.debug("[dbg] args.spec[{}].min       : {}", i, arg.min());
				Log.debug("[dbg] args.spec[{}].max       : {}", i, arg.max());
				Log.debug("[dbg] args.spec[{}].choices   : {}", i,
						String.join(", ", arg.choices() == null ? java.util.List.of() : arg.choices()));
				Log.debug("[dbg] args.spec[{}].pattern   : {}", i, nvl(arg.pattern()));
				Log.debug("[dbg] args.spec[{}].rest      : {}", i, arg.rest());
			}
		}

		var steps = s.steps();
		Log.debug("[dbg] steps.count      : {}", steps.size());
		for (int i = 0; i < steps.size(); i++) {
			var st = steps.get(i);
			Log.debug("[dbg] --- step[{}] ---------------------------------------------", i);
			Log.debug("[dbg] step[{}].command   : {}", i, st.command());
			Log.debug("[dbg] step[{}].run-as    : {}", i, st.runAs());
			Log.debug("[dbg] step[{}].delay     : {} ({} ms)", i, st.delay(), st.delay().toMillis());
			Log.debug("[dbg] step[{}].timeout   : {} ({} ms)", i, st.timeout(), st.timeout().toMillis());

			var tt = st.target();
			Log.debug("[dbg] step[{}].target.mode              : {}", i, tt.mode());
			Log.debug("[dbg] step[{}].target.fixed             : {}", i, nvl(tt.fixed()));
			Log.debug("[dbg] step[{}].target.arg-index         : {}", i, tt.argIndex());
			Log.debug("[dbg] step[{}].target.require-online    : {}", i, tt.requireOnline());
			Log.debug("[dbg] step[{}].target.require-on-server : {}", i, tt.requireOnServer());
		}

		Log.debug("[dbg] ----------------------------------------------------------------");
	}

	private static String nvl(String s) {
		return (s == null || s.isBlank()) ? "<null>" : s;
	}
}
