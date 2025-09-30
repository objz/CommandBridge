package dev.objz.commandbridge.main.scripting.v3.compile.nodes;

import dev.objz.commandbridge.main.scripting.v3.compile.api.NodeCompiler;
import dev.objz.commandbridge.main.scripting.v3.compile.core.CompileContext;
import dev.objz.commandbridge.main.scripting.v3.compile.core.Field;
import dev.objz.commandbridge.main.scripting.v3.compile.core.Path;
import dev.objz.commandbridge.main.scripting.v3.compile.core.ProblemSink;
import dev.objz.commandbridge.main.scripting.v3.compile.core.Schema;
import dev.objz.commandbridge.main.scripting.v3.compile.validate.Validators;
import dev.objz.commandbridge.main.scripting.v3.model.raw.DefaultsRaw;
import dev.objz.commandbridge.main.scripting.v3.model.resolved.Defaults;
import dev.objz.commandbridge.main.scripting.v3.model.resolved.Target;

public final class DefaultsCompiler implements NodeCompiler<DefaultsRaw, Defaults> {
	@Override
	public Defaults compile(DefaultsRaw raw, CompileContext ctx, ProblemSink p, Path path) {
		Target target = new TargetCompiler(true)
				.compile(raw == null ? null : raw.target, ctx, p, path.child("target"));
		var delay = new Field<>("delay", Schema.DEFAULT_DELAY).orDefault(raw == null ? null : raw.delay);
		var cooldown = new Field<>("cooldown", Schema.DEFAULT_COOLDOWN)
				.orDefault(raw == null ? null : raw.cooldown);

		Validators.nonNegative(delay, path, p, "delay");
		Validators.nonNegative(cooldown, path, p, "cooldown");

		return new Defaults(target, delay, cooldown);
	}
}
