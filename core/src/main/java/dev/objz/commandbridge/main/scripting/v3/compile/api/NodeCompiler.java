package dev.objz.commandbridge.main.scripting.v3.compile.api;

import dev.objz.commandbridge.main.scripting.v3.compile.core.CompileContext;
import dev.objz.commandbridge.main.scripting.v3.compile.core.Path;
import dev.objz.commandbridge.main.scripting.v3.compile.core.ProblemSink;

public interface NodeCompiler<R, T> {
	T compile(R raw, CompileContext ctx, ProblemSink problems, Path path);
}
