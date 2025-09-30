package dev.objz.commandbridge.main.scripting.v3.compile.core;

public record Problem(String path, Severity severity, String message) {
    @Override public String toString() {
        return "%s: %s - %s".formatted(severity, path == null ? "<root>" : path, message);
    }
}
