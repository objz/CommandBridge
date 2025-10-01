package dev.objz.commandbridge.velocity;

import dev.objz.commandbridge.main.logging.Log;
import dev.objz.commandbridge.main.scripting.v3.compiler.io.StepResolver;
import dev.objz.commandbridge.main.scripting.v3.model.domain.*;

import java.util.List;

/**
 * Prints a compact, readable debug dump of a compiled v3 Script.
 * Only logs when debug is enabled.
 */
public final class ScriptDebug {
	private ScriptDebug() {
	}

	public static void printScript(Script script, String sourceName) {
		if (!Log.isDebug() || script == null)
			return;

		Log.debug("=== SCRIPT DEBUG [{}] ===", sourceName);
		printHeader(script);
		printPermissions(script.permissions());
		printDefaults(script.defaults());
		printArgs(script.args());
		printSteps(script);
		Log.debug("=== END SCRIPT [{}] ===", sourceName);
	}

	// --- sections ---

	private static void printHeader(Script s) {
		Log.debug("Meta: version={}, enabled={}, name='{}'", s.version(), s.enabled(), s.name());
		Log.debug("      aliases={}, description={}", s.aliases(), nullToPlaceholder(s.description()));
	}

	private static void printPermissions(Permissions p) {
		if (p == null)
			return;
		Log.debug("Permissions: enabled={}, silent={}", p.enabled(), p.silent());
	}

	private static void printDefaults(Defaults d) {
		if (d == null)
			return;
		Log.debug("Defaults: delay={}, cooldown={}", d.delay(), d.cooldown());

		Target t = d.target();
		if (t == null) {
			Log.debug("Defaults.target: <null>");
			return;
		}
		TargetServer srv = t.server();
		TargetKind k = t.kind();
		Log.debug("Defaults.target: runAs={}, id='{}', kind[reg={}, exec={}]", t.runAs(),
				nullToPlaceholder(t.id()),
				k != null ? k.register() : null,
				k != null ? k.execute() : null);
		if (srv != null) {
			Log.debug("                 server[targetRequired={}, scheduleOnline={}, timeout={}, freq={}]",
					srv.targetRequired(), srv.scheduleOnline(), srv.timeout(), srv.frequency());
		}
	}

	private static void printArgs(List<Script.ArgDef> args) {
		if (args == null || args.isEmpty()) {
			Log.debug("Args: []");
			return;
		}
		for (int i = 0; i < args.size(); i++) {
			var a = args.get(i);
			Log.debug("Arg[{}]: name='{}', required={}, type={}", i, a.name(), a.required(), a.type());
		}
	}

	private static void printSteps(Script s) {
		List<CommandStep> steps = s.steps(); // field is 'steps' in current domain model
		if (steps == null || steps.isEmpty()) {
			Log.debug("Commands: []");
			return;
		}
		Log.debug("Commands: {} step(s)", steps.size());

		for (int i = 0; i < steps.size(); i++) {
			CommandStep step = steps.get(i);
			var resolved = StepResolver.resolve(s.defaults(), step);
			boolean oTarget = step.targetOverride() != null;
			boolean oDelay = step.delayOverride() != null;
			boolean oTimeout = step.timeoutOverride() != null;

			Log.debug("  - Step[{}]: cmd=\"{}\"", i, step.command());
			Log.debug("    Target{}: runAs={}, id='{}', kind[reg={}, exec={}']",
					flag(oTarget),
					resolved.target().runAs(),
					nullToPlaceholder(resolved.target().id()),
					resolved.target().kind() != null ? resolved.target().kind().register() : null,
					resolved.target().kind() != null ? resolved.target().kind().execute() : null);

			TargetServer srv = resolved.target().server();
			if (srv != null) {
				Log.debug("    Server{}: targetRequired={}, scheduleOnline={}, timeout{}={}, freq={}",
						flag(oTarget),
						srv.targetRequired(),
						srv.scheduleOnline(),
						flag(oTimeout),
						srv.timeout(),
						srv.frequency());
			}

			Log.debug("    Timings : delay{}={}, timeout{}={}",
					flag(oDelay), resolved.delay(),
					flag(oTimeout), resolved.timeout());
		}
	}

	// --- utils ---

	private static String flag(boolean overridden) {
		return overridden ? "*" : "";
	}

	private static String nullToPlaceholder(String s) {
		return (s == null || s.isBlank()) ? "<none>" : s;
	}
}
