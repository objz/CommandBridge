package dev.objz.commandbridge.main.scripting.v3.resolver;

import dev.objz.commandbridge.main.scripting.v3.effective.EffectiveModels;
import dev.objz.commandbridge.main.scripting.v3.enums.ExecutorMode;
import dev.objz.commandbridge.main.scripting.v3.enums.ScriptSide;
import dev.objz.commandbridge.main.scripting.v3.model.*;
import dev.objz.commandbridge.main.scripting.v3.util.DurationUtil;

import java.time.Duration;

public final class DefaultsResolver {
	public EffectiveModels.Permissions perms(PermissionsSpec p) {
		boolean enabled = p == null || p.enabled() == null ? true : p.enabled();
		boolean silent = p == null || p.silent() == null ? false : p.silent();
		return new EffectiveModels.Permissions(enabled, silent);
	}

	public EffectiveModels.Defaults defaults(DefaultsSpec d) {
		var tgt = target(d != null ? d.target() : null);
		Duration delay = DurationUtil.parseFlexible(d != null ? d.delay() : null, Duration.ZERO);
		Duration cooldown = DurationUtil.parseFlexible(d != null ? d.cooldown() : null, Duration.ZERO);
		Duration timeout = DurationUtil.parseFlexible(d != null ? d.timeout() : null, Duration.ofSeconds(2));
		return new EffectiveModels.Defaults(tgt, delay, cooldown, timeout);
	}

	public EffectiveModels.Target target(TargetDefaultsSpec t) {
		ExecutorMode runAs = t == null || t.runAs() == null ? ExecutorMode.PLAYER : t.runAs();
		String id = t == null ? null : t.id();
		ScriptSide reg = (t == null || t.kind() == null || t.kind().register() == null) ? ScriptSide.VELOCITY
				: t.kind().register();
		ScriptSide exe = (t == null || t.kind() == null || t.kind().execute() == null) ? ScriptSide.BACKEND
				: t.kind().execute();

		boolean targetReq = t == null || t.server() == null || t.server().targetRequired() == null ? true
				: t.server().targetRequired();
		boolean sched = t == null || t.server() == null || t.server().scheduleOnline() == null ? false
				: t.server().scheduleOnline();

		var timeout = DurationUtil.parseFlexible(
				t == null || t.server() == null ? null : t.server().timeoutEffective(),
				Duration.ofSeconds(1));
		var freq = DurationUtil.parseFlexible(
				t == null || t.server() == null ? null : t.server().frequencyEffective(),
				Duration.ofSeconds(2));
		return new EffectiveModels.Target(runAs, id, reg, exe, targetReq, sched, timeout, freq);
	}

	public EffectiveModels.Target mergeStepTarget(EffectiveModels.Target base, StepTargetSpec o) {
		if (o == null)
			return base;
		var runAs = o.runAs() != null ? o.runAs() : base.runAs();
		var id = o.id() != null ? o.id() : base.id();
		var reg = (o.kind() != null && o.kind().register() != null) ? o.kind().register() : base.register();
		var exe = (o.kind() != null && o.kind().execute() != null) ? o.kind().execute() : base.execute();

		Boolean trFlat = o.targetRequired();
		Boolean soFlat = o.scheduleOnline();
		Boolean trNested = o.server() != null ? o.server().targetRequired() : null;
		Boolean soNested = o.server() != null ? o.server().scheduleOnline() : null;

		boolean targetReq = pick(pick(trFlat, trNested), base.targetRequired());
		boolean sched = pick(pick(soFlat, soNested), base.scheduleOnline());

		var tout = o.server() != null ? o.server().timeoutEffective() : null;
		var freq = o.server() != null ? o.server().frequencyEffective() : null;
		var timeout = tout != null ? DurationUtil.parseFlexible(tout, base.scheduleTimeout())
				: base.scheduleTimeout();
		var frequency = freq != null ? DurationUtil.parseFlexible(freq, base.scheduleFrequency())
				: base.scheduleFrequency();

		return new EffectiveModels.Target(runAs, id, reg, exe, targetReq, sched, timeout, frequency);
	}

	private static <T> T pick(T a, T b) {
		return a != null ? a : b;
	}
}
