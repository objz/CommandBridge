package dev.objz.commandbridge.main.scripting.v3.compiler.compilers;

import dev.objz.commandbridge.main.scripting.v3.compiler.CompileContext;
import dev.objz.commandbridge.main.scripting.v3.compiler.NodeCompiler;
import dev.objz.commandbridge.main.scripting.v3.compiler.io.Validators;
import dev.objz.commandbridge.main.scripting.v3.compiler.problems.ProblemSink;
import dev.objz.commandbridge.main.scripting.v3.compiler.schema.Path;
import dev.objz.commandbridge.main.scripting.v3.model.domain.TargetKind;
import dev.objz.commandbridge.main.scripting.v3.model.dto.TargetKindDto;

public final class TargetKindCompiler implements NodeCompiler<TargetKindDto, TargetKind> {
	private final boolean isDefault;

	public TargetKindCompiler(boolean isDefault) {
		this.isDefault = isDefault;
	}

	@Override
	public TargetKind compile(TargetKindDto raw, CompileContext ctx, ProblemSink p, Path path) {
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
