package dev.objz.commandbridge.main.scripting.v3.compiler.compilers;

import dev.objz.commandbridge.main.scripting.v3.compiler.CompileContext;
import dev.objz.commandbridge.main.scripting.v3.compiler.NodeCompiler;
import dev.objz.commandbridge.main.scripting.v3.compiler.problems.ProblemSink;
import dev.objz.commandbridge.main.scripting.v3.compiler.schema.Field;
import dev.objz.commandbridge.main.scripting.v3.compiler.schema.Path;
import dev.objz.commandbridge.main.scripting.v3.compiler.schema.Schema;
import dev.objz.commandbridge.main.scripting.v3.model.domain.Permissions;
import dev.objz.commandbridge.main.scripting.v3.model.dto.PermissionsDto;

public final class PermissionsCompiler implements NodeCompiler<PermissionsDto, Permissions> {
	@Override
	public Permissions compile(PermissionsDto raw, CompileContext ctx, ProblemSink p, Path path) {
		var enabled = new Field<>("enabled", Schema.PERM_ENABLED).orDefault(raw == null ? null : raw.enabled);
		var silent = new Field<>("silent", Schema.PERM_SILENT).orDefault(raw == null ? null : raw.silent);
		return new Permissions(enabled, silent);
	}
}
