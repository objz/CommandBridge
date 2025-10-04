package dev.objz.commandbridge.scripting.bind;

import dev.objz.commandbridge.scripting.validation.ProblemSink;

public record BindContext(TypeAdapterRegistry adapters, ProblemSink problems) implements ConvertContext {}
