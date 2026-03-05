package dev.objz.commandbridge.scripting.migration;

/**
 *
 * @param type   the operation to perform (REMOVE, RENAME, ADD)
 * @param path   dot-separated path with optional [*] for list wildcards,
 *               e.g. "defaults.server.timeout" or "commands[*].server.timeout"
 * @param to     the new key name (only used for RENAME)
 * @param value  the raw YAML value text to insert after the colon (only used for ADD)
 * @param after  the sibling key to insert after (only used for ADD, optional)
 */
public record MigrationRule(Type type, String path, String to, String value, String after) {

    public enum Type {
        REMOVE,
        RENAME,
        ADD
    }

    public MigrationRule(Type type, String path) {
        this(type, path, null, null, null);
    }

    public MigrationRule(Type type, String path, String to) {
        this(type, path, to, null, null);
    }
}
