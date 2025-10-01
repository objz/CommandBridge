package dev.objz.commandbridge.main.scripting.v3.compiler.compilers;

import java.util.List;

import dev.objz.commandbridge.main.scripting.v3.compiler.CompileContext;
import dev.objz.commandbridge.main.scripting.v3.compiler.NodeCompiler;
import dev.objz.commandbridge.main.scripting.v3.compiler.problems.ProblemSink;
import dev.objz.commandbridge.main.scripting.v3.compiler.schema.Field;
import dev.objz.commandbridge.main.scripting.v3.compiler.schema.Path;
import dev.objz.commandbridge.main.scripting.v3.compiler.schema.Schema;
import dev.objz.commandbridge.main.scripting.v3.model.domain.Defaults;
import dev.objz.commandbridge.main.scripting.v3.model.domain.Permissions;
import dev.objz.commandbridge.main.scripting.v3.model.domain.Script;
import dev.objz.commandbridge.main.scripting.v3.model.dto.ArgDto;
import dev.objz.commandbridge.main.scripting.v3.model.dto.CommandStepDto;
import dev.objz.commandbridge.main.scripting.v3.model.dto.DefaultsDto;
import dev.objz.commandbridge.main.scripting.v3.model.dto.PermissionsDto;
import dev.objz.commandbridge.main.scripting.v3.model.dto.ScriptDto;

public final class ScriptNodeCompiler implements NodeCompiler<ScriptDto, Script> {
	@Override
	public Script compile(ScriptDto raw, CompileContext ctx, ProblemSink p, Path path) {
		var fVersion = new Field<>("version", Schema.VERSION);
		var fName = new Field<>("name", Schema.NAME);

		int version = fVersion.required(raw.version, p, path);
		String name = fName.required(raw.name, p, path);
		String description = new Field<>("description", Schema.DESCRIPTION).orDefault(raw.description);
		boolean enabled = new Field<>("enabled", Schema.ENABLED).orDefault(raw.enabled);
		var aliases = new Field<>("aliases", Schema.ALIASES).orDefault(raw.aliases);

		Permissions perm = ctx.compile(PermissionsDto.class, raw.permissions, p, path.child("permissions"));
		Defaults defs = ctx.compile(DefaultsDto.class, raw.defaults, p, path.child("defaults"));
		var args = ctx.<ArgDto.ListWrapper, List<Script.ArgDef>>compile(ArgDto.ListWrapper.class,
				new ArgDto.ListWrapper(raw.args), p, path.child("args"));

		var steps = ctx.<CommandStepDto.ListWrapper, List<dev.objz.commandbridge.main.scripting.v3.model.domain.CommandStep>>compile(
				CommandStepDto.ListWrapper.class, new CommandStepDto.ListWrapper(raw.commands), p,
				path.child("commands"));

		if (p.hasErrors())
			return null;
		return new Script(version, name, description, enabled, aliases, perm, defs, args, steps);
	}
}
