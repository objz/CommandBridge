package dev.objz.commandbridge.main.scripting.v3.compiler.compilers;

import dev.objz.commandbridge.main.scripting.v3.compiler.CompileContext;
import dev.objz.commandbridge.main.scripting.v3.compiler.NodeCompiler;
import dev.objz.commandbridge.main.scripting.v3.compiler.problems.ProblemSink;
import dev.objz.commandbridge.main.scripting.v3.compiler.schema.Path;
import dev.objz.commandbridge.main.scripting.v3.model.domain.CommandStep;
import dev.objz.commandbridge.main.scripting.v3.model.dto.CommandStepDto;

import java.util.ArrayList;
import java.util.List;

public final class CommandListCompiler implements NodeCompiler<CommandStepDto.ListWrapper, List<CommandStep>> {
	@Override
	public List<CommandStep> compile(CommandStepDto.ListWrapper raw, CompileContext ctx, ProblemSink p, Path path) {
		var out = new ArrayList<CommandStep>();
		if (raw == null || raw.list == null)
			return out;

		for (int i = 0; i < raw.list.size(); i++) {
			var ip = path.index(i);
			CommandStep step = ctx.compile(CommandStepDto.class, raw.list.get(i), p, ip);
			out.add(step);
		}
		return out;
	}
}
