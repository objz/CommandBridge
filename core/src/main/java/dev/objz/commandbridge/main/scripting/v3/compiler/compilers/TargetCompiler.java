package dev.objz.commandbridge.main.scripting.v3.compiler.compilers;

import dev.objz.commandbridge.main.scripting.v3.compiler.CompileContext;
import dev.objz.commandbridge.main.scripting.v3.compiler.NodeCompiler;
import dev.objz.commandbridge.main.scripting.v3.compiler.io.Validators;
import dev.objz.commandbridge.main.scripting.v3.compiler.problems.ProblemSink;
import dev.objz.commandbridge.main.scripting.v3.compiler.schema.Field;
import dev.objz.commandbridge.main.scripting.v3.compiler.schema.Path;
import dev.objz.commandbridge.main.scripting.v3.compiler.schema.Rule;
import dev.objz.commandbridge.main.scripting.v3.model.domain.Target;
import dev.objz.commandbridge.main.scripting.v3.model.domain.TargetKind;
import dev.objz.commandbridge.main.scripting.v3.model.domain.TargetServer;
import dev.objz.commandbridge.main.scripting.v3.model.dto.TargetDto;
import dev.objz.commandbridge.main.scripting.v3.model.dto.TargetKindDto;
import dev.objz.commandbridge.main.scripting.v3.model.dto.TargetServerDto;

public final class TargetCompiler implements NodeCompiler<TargetDto, Target> {
	private final boolean isDefault;

	public TargetCompiler(boolean isDefault) {
		this.isDefault = isDefault;
	}

	@Override
	public Target compile(TargetDto raw, CompileContext ctx, ProblemSink p, Path path) {
		if (raw == null) {
			if (isDefault) {
				p.error(path.toString(), "target is required in defaults");
			}
			TargetServer srv = ctx.compile(TargetServerDto.class, null, p, path.child("server"));
			return new Target(null, null, new TargetKind(null, null), srv);
		}

		Target.RunAs runAs = isDefault
				? Validators.requiredEnum(Target.RunAs.class, raw.runAs, p, path, "run-as")
				: (raw.runAs == null ? null
						: Validators.parseEnum(Target.RunAs.class, raw.runAs, p, path, "run-as",
								null));
		String id = isDefault
				? new Field<String>("id", Rule.isRequired()).required(raw.id, p, path)
				: raw.id;

		var kind = isDefault
				? new TargetKindCompiler(true).compile(raw.kind, ctx, p, path.child("kind"))
				: ctx.registry().get(TargetKindDto.class).compile(raw.kind, ctx, p, path.child("kind"));
		var server = ctx.compile(TargetServerDto.class, raw.server, p, path.child("server"));

		return new Target(runAs, id, (TargetKind) kind, (TargetServer) server);
	}
}
