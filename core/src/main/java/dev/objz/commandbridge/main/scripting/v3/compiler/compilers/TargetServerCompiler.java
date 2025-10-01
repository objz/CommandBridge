package dev.objz.commandbridge.main.scripting.v3.compiler.compilers;

import dev.objz.commandbridge.main.scripting.v3.compiler.CompileContext;
import dev.objz.commandbridge.main.scripting.v3.compiler.NodeCompiler;
import dev.objz.commandbridge.main.scripting.v3.compiler.io.Validators;
import dev.objz.commandbridge.main.scripting.v3.compiler.problems.ProblemSink;
import dev.objz.commandbridge.main.scripting.v3.compiler.schema.Field;
import dev.objz.commandbridge.main.scripting.v3.compiler.schema.Path;
import dev.objz.commandbridge.main.scripting.v3.compiler.schema.Schema;
import dev.objz.commandbridge.main.scripting.v3.model.domain.TargetServer;
import dev.objz.commandbridge.main.scripting.v3.model.dto.TargetServerDto;

public final class TargetServerCompiler implements NodeCompiler<TargetServerDto, TargetServer> {
	@Override
	public TargetServer compile(TargetServerDto raw, CompileContext ctx, ProblemSink p, Path path) {
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
