package dev.objz.commandbridge.main.scripting.v3.compile.core;

import dev.objz.commandbridge.main.scripting.v3.compile.api.Registry;

public record CompileContext(Registry registry, Schema schema) {
    public <R, T> T compile(Class<R> type, R raw, ProblemSink p, Path path) {
        var nc = registry.get(type); 
        return (T) nc.compile(raw, this, p, path);
    }
}
