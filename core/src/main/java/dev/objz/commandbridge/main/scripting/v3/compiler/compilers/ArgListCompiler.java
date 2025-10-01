package dev.objz.commandbridge.main.scripting.v3.compiler.compilers;

import java.util.ArrayList;
import java.util.List;

import dev.objz.commandbridge.main.scripting.v3.compiler.CompileContext;
import dev.objz.commandbridge.main.scripting.v3.compiler.NodeCompiler;
import dev.objz.commandbridge.main.scripting.v3.compiler.io.Validators;
import dev.objz.commandbridge.main.scripting.v3.compiler.problems.ProblemSink;
import dev.objz.commandbridge.main.scripting.v3.compiler.schema.Path;
import dev.objz.commandbridge.main.scripting.v3.model.domain.Script;
import dev.objz.commandbridge.main.scripting.v3.model.dto.ArgDto;

public final class ArgListCompiler implements NodeCompiler<ArgDto.ListWrapper, List<Script.ArgDef>> {
	@Override
	public List<Script.ArgDef> compile(ArgDto.ListWrapper raw, CompileContext ctx, ProblemSink p, Path path) {
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
