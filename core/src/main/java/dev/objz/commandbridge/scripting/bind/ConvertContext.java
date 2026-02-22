package dev.objz.commandbridge.scripting.bind;

import dev.objz.commandbridge.scripting.validation.ProblemSink;

public interface ConvertContext {
    TypeAdapterRegistry adapters();

    ProblemSink problems();
}
