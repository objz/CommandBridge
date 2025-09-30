package dev.objz.commandbridge.main.scripting.v3.compile.nodes;

import dev.objz.commandbridge.main.scripting.v3.compile.api.NodeCompiler;
import dev.objz.commandbridge.main.scripting.v3.compile.core.CompileContext;
import dev.objz.commandbridge.main.scripting.v3.compile.core.Path;
import dev.objz.commandbridge.main.scripting.v3.compile.core.ProblemSink;
import dev.objz.commandbridge.main.scripting.v3.compile.validate.Validators;
import dev.objz.commandbridge.main.scripting.v3.model.raw.TargetKindRaw;
import dev.objz.commandbridge.main.scripting.v3.model.resolved.TargetKind;

public final class TargetKindCompiler implements NodeCompiler<TargetKindRaw, TargetKind> {
	private final boolean isDefault;

	public TargetKindCompiler(boolean isDefault) {
		this.isDefault = isDefault;
	}

	@Override
	public TargetKind compile(TargetKindRaw raw, CompileContext ctx, ProblemSink p, Path path) {
		if (raw == null) {
			if (isDefault) {
				p.error(path.toString(), "kind is required in defaults");
			}
			return new TargetKind(null, null);
		}

		var reg = isDefault
				? Validators.requiredEnum(TargetKind.Type.class, raw.register, p, path, "register")
				: (raw.register == null ? null
						: Validators.parseEnum(TargetKind.Type.class, raw.register, p, path,
								"register", null));

		var exe = isDefault
				? Validators.requiredEnum(TargetKind.Type.class, raw.execute, p, path, "execute")
				: (raw.execute == null ? null
						: Validators.parseEnum(TargetKind.Type.class, raw.execute, p, path,
								"execute", null));

		return new TargetKind(reg, exe);
	}
}
