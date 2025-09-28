package dev.objz.commandbridge.main.scripting.v3.api.groups.commands;

import dev.objz.commandbridge.main.scripting.v3.api.ProblemSink;
import dev.objz.commandbridge.main.scripting.v3.api.groups.Defaults;
import dev.objz.commandbridge.main.scripting.v3.api.groups.Script;
import dev.objz.commandbridge.main.scripting.v3.api.groups.defaults.Target;
import dev.objz.commandbridge.main.scripting.v3.api.groups.defaults.TargetKind;
import dev.objz.commandbridge.main.scripting.v3.api.groups.defaults.TargetServer;

public final class StepResolver {

	private StepResolver() {
	}

	public static ResolvedStep resolve(CommandStep step, Script script, ProblemSink problems, int index) {
		Defaults base = script.defaults();

		Target baseTarget = base.target();
		Target.RunAs baseRunAs = baseTarget.runAs().effectiveValue();
		String baseId = baseTarget.id().effectiveValue();
		TargetKind baseKind = baseTarget.targetKind();
		TargetKind.Type baseRegister = baseKind.register().effectiveValue();
		TargetKind.Type baseExecute = baseKind.execute().effectiveValue();
		TargetServer baseServer = baseTarget.targetServer();
		boolean baseTargetRequired = Boolean.TRUE.equals(baseServer.targetRequired().effectiveValue());
		boolean baseScheduleOnline = Boolean.TRUE.equals(baseServer.scheduleOnline().effectiveValue());
		var baseTimeout = baseServer.timeout().effectiveValue();
		var baseFrequency = baseServer.frequency().effectiveValue();

		var outRunAs = baseRunAs;
		var outId = baseId;
		var outRegister = baseRegister;
		var outExecute = baseExecute;
		var outTargetRequired = baseTargetRequired;
		var outScheduleOnline = baseScheduleOnline;
		var outTimeout = baseTimeout;
		var outFrequency = baseFrequency;
		var outDelay = base.delay();
		var outCooldown = base.cooldown();

		CommandOverrides ov = step.overrides();
		if (ov != null) {
			if (ov.delay() != null)
				outDelay = ov.delay();
			if (ov.cooldown() != null)
				outCooldown = ov.cooldown();
			if (ov.target() != null) {
				var t = ov.target();
				if (t.runAs() != null)
					outRunAs = t.runAs();
				if (t.id() != null && !t.id().isBlank())
					outId = t.id();

				if (t.kind() != null) {
					if (t.kind().register() != null)
						outRegister = t.kind().register();
					if (t.kind().execute() != null)
						outExecute = t.kind().execute();
				}
				if (t.server() != null) {
					if (t.server().targetRequired() != null)
						outTargetRequired = t.server().targetRequired();
					if (t.server().scheduleOnline() != null)
						outScheduleOnline = t.server().scheduleOnline();
					if (t.server().timeout() != null)
						outTimeout = t.server().timeout();
					if (t.server().frequency() != null)
						outFrequency = t.server().frequency();
				}
			}
		}
		//damn ugly code

		String cmd = step.command().effectiveValue();

		if (outId == null || outId.isBlank()) {
			problems.error("commands[" + index + "].target.id",
					"Target id must not be empty after overrides");
		}
		if (outRegister == null || outExecute == null) {
			problems.error("commands[" + index + "].target.kind",
					"Both 'register' and 'execute' must be set");
		}

		return new ResolvedStep(
				cmd,
				outRunAs,
				outId,
				outRegister,
				outExecute,
				outTargetRequired,
				outScheduleOnline,
				outTimeout,
				outFrequency,
				outDelay,
				outCooldown);
	}
}
