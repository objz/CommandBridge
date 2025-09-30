package dev.objz.commandbridge.main.scripting.v3.resolve;

import dev.objz.commandbridge.main.scripting.v3.compile.core.Path;
import dev.objz.commandbridge.main.scripting.v3.compile.core.ProblemSink;
import dev.objz.commandbridge.main.scripting.v3.model.resolved.*;

import java.time.Duration;
import java.util.Objects;

public final class StepResolver {

	public record ResolvedStep(
			String command,
			Target target,
			Duration delay,
			Duration timeout) {
	}

	public static ResolvedStep resolve(
			Script script,
			CommandStep step,
			ProblemSink problems,
			int index) {
		Objects.requireNonNull(script);
		Objects.requireNonNull(step);

		var path = Path.root().child("commands").index(index);

		var baseTarget = script.defaults().target();
		var baseDelay = script.defaults().delay();
		var baseTimeout = script.defaults().target().server().timeout();

		var ov = step.overrides();

		var resolvedTarget = ov != null && ov.target() != null
				? applyTargetOverride(baseTarget, ov.target())
				: baseTarget;

		var resolvedDelay = pick(ov == null ? null : ov.delay(), baseDelay);
		var resolvedTimeout = pick(ov == null ? null : ov.timeout(), baseTimeout);

		if (resolvedTimeout != null && resolvedTimeout.isZero()) {
			problems.warn(path.child("timeout").toString(),
					"timeout is zero; command may be skipped or fail fast");
		}
		if (resolvedDelay != null && resolvedDelay.isNegative()) {
			problems.error(path.child("delay").toString(), "delay must not be negative");
		}

		return new ResolvedStep(step.command(), resolvedTarget, resolvedDelay, resolvedTimeout);
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
