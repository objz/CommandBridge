package dev.objz.commandbridge.main.scripting.v3.compile.nodes;

import java.util.ArrayList;
import java.util.List;

import dev.objz.commandbridge.main.scripting.v3.compile.api.NodeCompiler;
import dev.objz.commandbridge.main.scripting.v3.compile.core.CompileContext;
import dev.objz.commandbridge.main.scripting.v3.compile.core.Field;
import dev.objz.commandbridge.main.scripting.v3.compile.core.Path;
import dev.objz.commandbridge.main.scripting.v3.compile.core.ProblemSink;
import dev.objz.commandbridge.main.scripting.v3.compile.core.Schema;
import dev.objz.commandbridge.main.scripting.v3.model.raw.CommandStepRaw;
import dev.objz.commandbridge.main.scripting.v3.model.raw.TargetRaw;
import dev.objz.commandbridge.main.scripting.v3.model.resolved.CommandOverrides;
import dev.objz.commandbridge.main.scripting.v3.model.resolved.CommandStep;
import dev.objz.commandbridge.main.scripting.v3.model.resolved.Target;

public final class CommandListCompiler implements NodeCompiler<CommandStepRaw.ListWrapper, List<CommandStep>> {
	@Override
	public List<CommandStep> compile(CommandStepRaw.ListWrapper raw, CompileContext ctx, ProblemSink p, Path path) {
		var out = new ArrayList<CommandStep>();
		if (raw == null || raw.list == null || raw.list.isEmpty()) {
			p.error(path.toString(), "at least one command step is required");
			return out;
		}
		for (int i = 0; i < raw.list.size(); i++) {
			var s = raw.list.get(i);
			var ip = path.index(i);

			var cmd = new Field<>("command", Schema.STEP_COMMAND).required(s.command, p, ip);
			Target ovTarget = (s.target == null) ? null
					: ctx.compile(TargetRaw.class, s.target, p, ip.child("target"));

			var delay = s.delay;
			var timeout = s.timeout;
			if (delay != null && delay.isNegative())
				p.error(ip.child("delay").toString(), "must not be negative");
			if (timeout != null && (timeout.isZero() || timeout.isNegative()))
				p.error(ip.child("timeout").toString(), "must be positive");

			out.add(new CommandStep(cmd, new CommandOverrides(ovTarget, delay, timeout)));
		}
		return out;
	}
}
