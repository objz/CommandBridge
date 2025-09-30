package dev.objz.commandbridge.main.scripting.v3.compile.nodes;

import dev.objz.commandbridge.main.scripting.v3.compile.api.NodeCompiler;
import dev.objz.commandbridge.main.scripting.v3.compile.core.CompileContext;
import dev.objz.commandbridge.main.scripting.v3.compile.core.Field;
import dev.objz.commandbridge.main.scripting.v3.compile.core.Path;
import dev.objz.commandbridge.main.scripting.v3.compile.core.ProblemSink;
import dev.objz.commandbridge.main.scripting.v3.compile.core.Schema;
import dev.objz.commandbridge.main.scripting.v3.model.raw.PermissionsRaw;
import dev.objz.commandbridge.main.scripting.v3.model.resolved.Permissions;

public final class PermissionsCompiler implements NodeCompiler<PermissionsRaw, Permissions> {
	@Override
	public Permissions compile(PermissionsRaw raw, CompileContext ctx, ProblemSink p, Path path) {
		var enabled = new Field<>("enabled", Schema.PERM_ENABLED).orDefault(raw == null ? null : raw.enabled);
		var silent = new Field<>("silent", Schema.PERM_SILENT).orDefault(raw == null ? null : raw.silent);
		return new Permissions(enabled, silent);
	}
}
