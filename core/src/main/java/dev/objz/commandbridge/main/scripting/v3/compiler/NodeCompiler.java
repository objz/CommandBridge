package dev.objz.commandbridge.main.scripting.v3.compiler;

import dev.objz.commandbridge.main.scripting.v3.compiler.problems.ProblemSink;
import dev.objz.commandbridge.main.scripting.v3.compiler.schema.Path;

public interface NodeCompiler<R, T> {
	T compile(R raw, CompileContext ctx, ProblemSink problems, Path path);
}
