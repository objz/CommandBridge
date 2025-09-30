package dev.objz.commandbridge.main.scripting.v3.compile.nodes;

import dev.objz.commandbridge.main.scripting.v3.compile.api.NodeCompiler;
import dev.objz.commandbridge.main.scripting.v3.compile.core.CompileContext;
import dev.objz.commandbridge.main.scripting.v3.compile.core.Field;
import dev.objz.commandbridge.main.scripting.v3.compile.core.Path;
import dev.objz.commandbridge.main.scripting.v3.compile.core.ProblemSink;
import dev.objz.commandbridge.main.scripting.v3.compile.core.Rule;
import dev.objz.commandbridge.main.scripting.v3.compile.validate.Validators;
import dev.objz.commandbridge.main.scripting.v3.model.raw.TargetKindRaw;
import dev.objz.commandbridge.main.scripting.v3.model.raw.TargetRaw;
import dev.objz.commandbridge.main.scripting.v3.model.raw.TargetServerRaw;
import dev.objz.commandbridge.main.scripting.v3.model.resolved.Target;
import dev.objz.commandbridge.main.scripting.v3.model.resolved.TargetKind;
import dev.objz.commandbridge.main.scripting.v3.model.resolved.TargetServer;

public final class TargetCompiler implements NodeCompiler<TargetRaw, Target> {
	private final boolean isDefault;

	public TargetCompiler(boolean isDefault) {
		this.isDefault = isDefault;
	}

	@Override
	public Target compile(TargetRaw raw, CompileContext ctx, ProblemSink p, Path path) {
		if (raw == null) {
			if (isDefault) {
				p.error(path.toString(), "target is required in defaults");
			}
			TargetServer srv = ctx.compile(TargetServerRaw.class, null, p, path.child("server"));
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
				: ctx.registry().get(TargetKindRaw.class).compile(raw.kind, ctx, p, path.child("kind"));
		var server = ctx.compile(TargetServerRaw.class, raw.server, p, path.child("server"));

		return new Target(runAs, id, (TargetKind) kind, (TargetServer) server);
	}
}
