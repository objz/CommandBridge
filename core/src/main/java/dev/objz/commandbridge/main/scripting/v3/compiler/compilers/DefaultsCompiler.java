package dev.objz.commandbridge.main.scripting.v3.compiler.compilers;

import dev.objz.commandbridge.main.scripting.v3.compiler.CompileContext;
import dev.objz.commandbridge.main.scripting.v3.compiler.NodeCompiler;
import dev.objz.commandbridge.main.scripting.v3.compiler.io.Validators;
import dev.objz.commandbridge.main.scripting.v3.compiler.problems.ProblemSink;
import dev.objz.commandbridge.main.scripting.v3.compiler.schema.Field;
import dev.objz.commandbridge.main.scripting.v3.compiler.schema.Path;
import dev.objz.commandbridge.main.scripting.v3.compiler.schema.Schema;
import dev.objz.commandbridge.main.scripting.v3.model.domain.Defaults;
import dev.objz.commandbridge.main.scripting.v3.model.domain.Target;
import dev.objz.commandbridge.main.scripting.v3.model.dto.DefaultsDto;

public final class DefaultsCompiler implements NodeCompiler<DefaultsDto, Defaults> {
	@Override
	public Defaults compile(DefaultsDto raw, CompileContext ctx, ProblemSink p, Path path) {
		Target target = new TargetCompiler(true).compile(raw == null ? null : raw.target, ctx, p,
				path.child("target"));
		var delay = new Field<>("delay", Schema.DEFAULT_DELAY).orDefault(raw == null ? null : raw.delay);
		var cooldown = new Field<>("cooldown", Schema.DEFAULT_COOLDOWN)
				.orDefault(raw == null ? null : raw.cooldown);

		Validators.nonNegative(delay, path, p, "delay");
		Validators.nonNegative(cooldown, path, p, "cooldown");

		return new Defaults(target, delay, cooldown);
	}
}
