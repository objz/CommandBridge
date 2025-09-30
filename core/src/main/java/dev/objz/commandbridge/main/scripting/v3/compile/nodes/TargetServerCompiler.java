package dev.objz.commandbridge.main.scripting.v3.compile.nodes;

import dev.objz.commandbridge.main.scripting.v3.compile.api.NodeCompiler;
import dev.objz.commandbridge.main.scripting.v3.compile.core.CompileContext;
import dev.objz.commandbridge.main.scripting.v3.compile.core.Field;
import dev.objz.commandbridge.main.scripting.v3.compile.core.Path;
import dev.objz.commandbridge.main.scripting.v3.compile.core.ProblemSink;
import dev.objz.commandbridge.main.scripting.v3.compile.core.Schema;
import dev.objz.commandbridge.main.scripting.v3.compile.validate.Validators;
import dev.objz.commandbridge.main.scripting.v3.model.raw.TargetServerRaw;
import dev.objz.commandbridge.main.scripting.v3.model.resolved.TargetServer;

public final class TargetServerCompiler implements NodeCompiler<TargetServerRaw, TargetServer> {
	@Override
	public TargetServer compile(TargetServerRaw raw, CompileContext ctx, ProblemSink p, Path path) {
		var req = new Field<>("target-required", Schema.TARGET_SERVER_REQUIRED)
				.orDefault(raw == null ? null : raw.targetRequired);
		var sch = new Field<>("schedule-online", Schema.TARGET_SERVER_SCHEDULE)
				.orDefault(raw == null ? null : raw.scheduleOnline);
		var to = new Field<>("timeout", Schema.TARGET_SERVER_TIMEOUT)
				.orDefault(raw == null ? null : raw.timeout);
		var fr = new Field<>("frequency", Schema.TARGET_SERVER_FREQ)
				.orDefault(raw == null ? null : raw.frequency);

		Validators.positive(to, path, p, "timeout");
		Validators.positive(fr, path, p, "frequency");

		return new TargetServer(req, sch, to, fr);
	}
}
