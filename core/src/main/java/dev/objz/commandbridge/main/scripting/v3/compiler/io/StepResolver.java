package dev.objz.commandbridge.main.scripting.v3.compiler.io;

import dev.objz.commandbridge.main.scripting.v3.model.domain.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public final class StepResolver {

	public record ResolvedStep(
			String command,
			Target target,
			Duration delay,
			Duration timeout) {
	}

	public static ResolvedStep resolve(Defaults defaults, CommandStep step) {
		Target baseTarget = defaults.target();
		Target effectiveTarget = (step.targetOverride() != null)
				? applyTargetOverride(baseTarget, step.targetOverride())
				: baseTarget;

		Duration delay = pick(step.delayOverride(), defaults.delay());
		Duration timeout = pick(step.timeoutOverride(), defaults.target().server().timeout());

		return new ResolvedStep(step.command(), effectiveTarget, delay, timeout);
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
		return overrideValue != null ? overrideValue : base;
	}

	private static Target applyTargetOverride(Target base, Target overrideTarget) {
		var runAs = overrideTarget.runAs() != null ? overrideTarget.runAs() : base.runAs();
		var id = overrideTarget.id() != null ? overrideTarget.id() : base.id();

		var kind = mergeKind(base.kind(), overrideTarget.kind());
		var srv = mergeServer(base.server(), overrideTarget.server());

		return new Target(runAs, id, kind, srv);
	}

	private static TargetKind mergeKind(TargetKind base, TargetKind ov) {
		if (ov == null)
			return base;
		var reg = ov.register() != null ? ov.register() : base.register();
		var exe = ov.execute() != null ? ov.execute() : base.execute();
		return new TargetKind(reg, exe);
	}

	private static TargetServer mergeServer(TargetServer base, TargetServer ov) {
		if (ov == null)
			return base;
		var req = ov.targetRequired();
		var sch = ov.scheduleOnline();
		var to = ov.timeout() != null ? ov.timeout() : base.timeout();
		var fr = ov.frequency() != null ? ov.frequency() : base.frequency();
		return new TargetServer(req, sch, to, fr);
	}

	private StepResolver() {
	}
}
