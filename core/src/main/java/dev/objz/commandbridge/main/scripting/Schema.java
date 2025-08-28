package dev.objz.commandbridge.main.scripting;

import dev.objz.commandbridge.main.scripting.ScriptTypes.*;

import java.util.*;

public final class Schema {
    private Schema() {}

    public static final class Registry {
        private final Map<FieldPath, FieldRule<?>> rules = new LinkedHashMap<>();

        public Registry() {
            // top level
            put("version", FieldRule.required());
            put("kind", FieldRule.required());
            put("name", FieldRule.required());
            put("enabled", FieldRule.optional(ctx -> Boolean.TRUE));
            put("description", FieldRule.optional(ctx -> ""));
            put("aliases", FieldRule.optional(ctx -> List.of()));

            // permissions
            put("permissions.enabled", FieldRule.optional(ctx -> Boolean.TRUE));
            put("permissions.silent", FieldRule.optional(ctx -> Boolean.FALSE));

            // run-as
            put("run-as", FieldRule.optional(ctx -> ExecutorMode.PLAYER));

            // target
            put("target.mode", FieldRule.optional(ctx -> TargetMode.PLAYERS_SERVER));
            put("target.fixed", FieldRule.optional(ctx -> null));
            put("target.arg-index", FieldRule.optional(ctx -> null));
            put("target.require-online", FieldRule.optional(ctx -> Boolean.TRUE));
            put("target.require-on-server", FieldRule.optional(ctx -> Boolean.TRUE));

            // defaults
            put("defaults.delay", FieldRule.optional(ctx -> "0s"));
            put("defaults.timeout", FieldRule.optional(ctx -> "2s"));
            put("defaults.cooldown", FieldRule.optional(ctx -> "0s"));
            put("defaults.rate-limit", FieldRule.optional(ctx -> null));

            // args
            put("args.description", FieldRule.optional(ctx -> ""));
            put("args.spec", FieldRule.optional(ctx -> List.of()));

            // args.spec[]
            put("args.spec[].name", FieldRule.optional(ctx -> null));
            put("args.spec[].index", FieldRule.optional(ctx -> null));
            put("args.spec[].required", FieldRule.optional(ctx -> Boolean.TRUE));
            put("args.spec[].type", FieldRule.optional(ctx -> ArgType.WORD));
            put("args.spec[].min", FieldRule.optional(ctx -> null));
            put("args.spec[].max", FieldRule.optional(ctx -> null));
            put("args.spec[].choices", FieldRule.optional(ctx -> List.of()));
            put("args.spec[].pattern", FieldRule.optional(ctx -> null));
            put("args.spec[].rest", FieldRule.optional(ctx -> Boolean.FALSE));

            // commands
            put("commands", FieldRule.required());
            put("commands[].command", FieldRule.required());
            put("commands[].run-as", FieldRule.optional(ctx -> null));
            put("commands[].delay", FieldRule.optional(ctx -> null));
            put("commands[].timeout", FieldRule.optional(ctx -> null));

            // commands[].target
            put("commands[].target.mode", FieldRule.optional(ctx -> null));
            put("commands[].target.fixed", FieldRule.optional(ctx -> null));
            put("commands[].target.arg-index", FieldRule.optional(ctx -> null));
            put("commands[].target.require-online", FieldRule.optional(ctx -> null));
            put("commands[].target.require-on-server", FieldRule.optional(ctx -> null));
        }

        public <T> void setRule(String path, FieldRule<T> rule) {
            rules.put(new FieldPath(path), rule);
        }

        public Optional<FieldRule<?>> ruleFor(String path) {
            for (var e : rules.entrySet()) {
                if (e.getKey().matches(path)) return Optional.of(e.getValue());
            }
            return Optional.empty();
        }

        public FieldRule.Context ctx(String scriptName) {
            return new FieldRule.Context(scriptName);
        }

        private void put(String path, FieldRule<?> rule) { rules.put(new FieldPath(path), rule); }
    }

    static final class FieldPath {
        private final String path;
        FieldPath(String path) { this.path = Objects.requireNonNull(path); }
        boolean matches(String other) {
            String p = path.replace("[]", "\\[[0-9]+\\]");
            return other.equals(path) || other.matches(p);
        }
        @Override public int hashCode() { return path.hashCode(); }
        @Override public boolean equals(Object o) { return o instanceof FieldPath f && f.path.equals(path); }
        @Override public String toString() { return path; }
    }

    public static final class FieldRule<T> {
        private final boolean required;
        private final java.util.function.Function<Context, T> defaultProvider;

        public FieldRule(boolean required, java.util.function.Function<Context, T> defaultProvider) {
            this.required = required; this.defaultProvider = defaultProvider;
        }
        public boolean isRequired() { return required; }
        public T defaultValue(Context ctx) { return defaultProvider == null ? null : defaultProvider.apply(ctx); }

        public static <T> FieldRule<T> required() { return new FieldRule<>(true, null); }
        public static <T> FieldRule<T> optional(java.util.function.Function<Context, T> def) { return new FieldRule<>(false, def); }

        public record Context(String scriptName) {}
    }
}
