package dev.objz.commandbridge.main.scripting.v3.compile.nodes;

import dev.objz.commandbridge.main.scripting.v3.compile.api.NodeCompiler;
import dev.objz.commandbridge.main.scripting.v3.compile.core.CompileContext;
import dev.objz.commandbridge.main.scripting.v3.compile.core.Field;
import dev.objz.commandbridge.main.scripting.v3.compile.core.Path;
import dev.objz.commandbridge.main.scripting.v3.compile.core.ProblemSink;
import dev.objz.commandbridge.main.scripting.v3.compile.core.Schema;
import dev.objz.commandbridge.main.scripting.v3.model.raw.ArgRaw;
import dev.objz.commandbridge.main.scripting.v3.model.raw.CommandStepRaw;
import dev.objz.commandbridge.main.scripting.v3.model.raw.DefaultsRaw;
import dev.objz.commandbridge.main.scripting.v3.model.raw.PermissionsRaw;
import dev.objz.commandbridge.main.scripting.v3.model.raw.ScriptRaw;
import dev.objz.commandbridge.main.scripting.v3.model.resolved.CommandStep;
import dev.objz.commandbridge.main.scripting.v3.model.resolved.Defaults;
import dev.objz.commandbridge.main.scripting.v3.model.resolved.Permissions;
import dev.objz.commandbridge.main.scripting.v3.model.resolved.Script;

public final class ScriptCompiler implements NodeCompiler<ScriptRaw, Script> {
	@Override
	public Script compile(ScriptRaw raw, CompileContext ctx, ProblemSink p, Path path) {
		var fVersion = new Field<>("version", Schema.VERSION);
		var fName = new Field<>("name", Schema.NAME);

		int version = fVersion.required(raw.version, p, path);
		String name = fName.required(raw.name, p, path);
		String description = new Field<>("description", Schema.DESCRIPTION).orDefault(raw.description);
		boolean enabled = new Field<>("enabled", Schema.ENABLED).orDefault(raw.enabled);
		var aliases = new Field<>("aliases", Schema.ALIASES).orDefault(raw.aliases);

		Permissions perm = ctx.compile(PermissionsRaw.class, raw.permissions, p, path.child("permissions"));
		Defaults defs = ctx.compile(DefaultsRaw.class, raw.defaults, p, path.child("defaults"));
		var args = ctx.<ArgRaw.ListWrapper, java.util.List<Script.ArgDef>>compile(ArgRaw.ListWrapper.class,
				new ArgRaw.ListWrapper(raw.args), p, path.child("args"));
		java.util.List<CommandStep> steps = ctx.compile(
				CommandStepRaw.ListWrapper.class, new CommandStepRaw.ListWrapper(raw.commands),
				p, path.child("commands"));

		if (p.hasErrors())
			return null;
		return new Script(version, name, description, enabled, aliases, perm, defs, args, steps);
	}
}
