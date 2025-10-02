package dev.objz.commandbridge.main.scripting.v3.compiler.io;

import dev.objz.commandbridge.main.scripting.v3.model.domain.CommandStep;
import dev.objz.commandbridge.main.scripting.v3.model.domain.Defaults;
import dev.objz.commandbridge.main.scripting.v3.model.domain.TargetKind;
import dev.objz.commandbridge.main.scripting.v3.model.domain.TargetServer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class StepResolver {

	public record ResolvedStep(
			String command,
			Defaults.RunAs runAs,
			String id,
			TargetKind kind,
			TargetServer server,
			Duration delay,
			Duration cooldown) {
	}

	public static ResolvedStep resolve(Defaults defaults, CommandStep step) {
		Objects.requireNonNull(defaults, "defaults");
		Objects.requireNonNull(step, "step");

		var baseRunAs = defaults.runAs();
		var baseId = defaults.id();
		var baseKind = defaults.kind();
		var baseSrv = defaults.server();

		var effKind = mergeKind(baseKind, step.kindOverride());
		var effSrv = mergeServer(baseSrv, step.serverOverride());

		var effDelay = pick(step.delayOverride(), defaults.delay());

		var effCooldown = defaults.cooldown();

		return new ResolvedStep(step.command(), baseRunAs, baseId, effKind, effSrv, effDelay, effCooldown);
	}

	public static List<ResolvedStep> resolveAll(Defaults defaults, List<CommandStep> steps) {
		var out = new ArrayList<ResolvedStep>();
		if (steps == null || steps.isEmpty())
			return out;
		for (var s : steps)
			out.add(resolve(defaults, s));
		return out;
	}

	private static Duration pick(Duration overrideValue, Duration base) {
		return (overrideValue != null) ? overrideValue : base;
	}

	private static TargetKind mergeKind(TargetKind base, TargetKind ov) {
		if (base == null && ov == null)
			return null;
		if (base == null)
			return new TargetKind(ov.register(), ov.execute());
		if (ov == null)
			return base;
		var reg = (ov.register() != null) ? ov.register() : base.register();
		var exe = (ov.execute() != null) ? ov.execute() : base.execute();
		return new TargetKind(reg, exe);
	}

	private static TargetServer mergeServer(TargetServer base, TargetServer ov) {
		if (base == null && ov == null)
			return null;
		if (ov == null)
			return base;

		boolean req = ov.targetRequired();
		boolean sch = ov.scheduleOnline();
		var timeout = (ov.timeout() != null) ? ov.timeout() : (base != null ? base.timeout() : null);
		var freq = (ov.frequency() != null) ? ov.frequency() : (base != null ? base.frequency() : null);

		return new TargetServer(req, sch, timeout, freq);
	}

	private StepResolver() {
	}
}
