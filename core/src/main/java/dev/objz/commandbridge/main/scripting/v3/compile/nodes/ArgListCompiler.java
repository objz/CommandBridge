package dev.objz.commandbridge.main.scripting.v3.compile.nodes;

import java.util.ArrayList;
import java.util.List;

import dev.objz.commandbridge.main.scripting.v3.compile.api.NodeCompiler;
import dev.objz.commandbridge.main.scripting.v3.compile.core.CompileContext;
import dev.objz.commandbridge.main.scripting.v3.compile.core.Path;
import dev.objz.commandbridge.main.scripting.v3.compile.core.ProblemSink;
import dev.objz.commandbridge.main.scripting.v3.compile.validate.Validators;
import dev.objz.commandbridge.main.scripting.v3.model.raw.ArgRaw;
import dev.objz.commandbridge.main.scripting.v3.model.resolved.Script;

public final class ArgListCompiler implements NodeCompiler<ArgRaw.ListWrapper, List<Script.ArgDef>> {
	@Override
	public List<Script.ArgDef> compile(ArgRaw.ListWrapper raw, CompileContext ctx, ProblemSink p, Path path) {
		var out = new ArrayList<Script.ArgDef>();
		if (raw == null || raw.list == null)
			return out;

		for (int i = 0; i < raw.list.size(); i++) {
			var a = raw.list.get(i);
			var ip = path.index(i);

			var name = (a.name == null || a.name.isBlank()) ? "unknown" : a.name;
			if (a.name == null || a.name.isBlank())
				p.error(ip.child("name").toString(), "is required");

			var typeStr = (a.type == null || a.type.isBlank()) ? "STRING" : a.type;
			var typ = Validators.parseEnum(Script.ArgType.class, typeStr, p, ip, "type",
					Script.ArgType.STRING);

			out.add(new Script.ArgDef(name, Boolean.TRUE.equals(a.required), typ));
		}
		return out;
	}
}
