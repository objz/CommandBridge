package dev.objz.commandbridge.scripting.bind;

import dev.objz.commandbridge.scripting.process.ProblemSink;

public interface ConvertContext {
	TypeAdapterRegistry adapters();

	ProblemSink problems();
}
