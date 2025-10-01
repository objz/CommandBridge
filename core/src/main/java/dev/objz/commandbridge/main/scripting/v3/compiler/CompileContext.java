package dev.objz.commandbridge.main.scripting.v3.compiler;

import dev.objz.commandbridge.main.scripting.v3.compiler.problems.ProblemSink;
import dev.objz.commandbridge.main.scripting.v3.compiler.schema.Path;
import dev.objz.commandbridge.main.scripting.v3.compiler.schema.Schema;

public record CompileContext(CompilerRegistry registry, Schema schema) {
	public <R, T> T compile(Class<R> type, R raw, ProblemSink p, Path path) {
		var nc = registry.get(type);
		return (T) nc.compile(raw, this, p, path);
	}
}
