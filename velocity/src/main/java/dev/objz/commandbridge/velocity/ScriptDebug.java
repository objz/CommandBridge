package dev.objz.commandbridge.velocity;

import dev.objz.commandbridge.main.logging.Log;
import dev.objz.commandbridge.main.scripting.v3.model.resolved.*;

import java.time.Duration;
import java.util.List;

/**
 * Debug utility to print all values of a loaded script, including resolved command overrides.
 */
public final class ScriptDebug {
    private ScriptDebug() {}

    public static void printScript(Script script, String sourceName) {
        if (!Log.isDebug()) {
            return; // Only print in debug mode
        }

        Log.debug("=== SCRIPT DEBUG: {} ===", sourceName);
        
        // Basic script info
        Log.debug("Script.version     : {}", script.version());
        Log.debug("Script.name        : '{}'", script.name());
        Log.debug("Script.description : '{}'", script.description() != null ? script.description() : "<null>");
        Log.debug("Script.enabled     : {}", script.enabled());
        Log.debug("Script.aliases     : {}", formatStringList(script.aliases()));

        // Permissions
        Log.debug("--- Permissions ---");
        Permissions perm = script.permissions();
        Log.debug("Permissions.enabled : {}", perm.enabled());
        Log.debug("Permissions.silent  : {}", perm.silent());

        // Defaults
        Log.debug("--- Defaults ---");
        Defaults def = script.defaults();
        printTarget(def.target(), "Defaults.target");
        Log.debug("Defaults.delay      : {}", formatDuration(def.delay()));
        Log.debug("Defaults.cooldown   : {}", formatDuration(def.cooldown()));

        // Args
        Log.debug("--- Args ({}) ---", script.args().size());
        List<Script.ArgDef> args = script.args();
        if (args.isEmpty()) {
            Log.debug("  <no arguments>");
        } else {
            for (int i = 0; i < args.size(); i++) {
                Script.ArgDef arg = args.get(i);
                Log.debug("  args[{}].name     : '{}'", i, arg.name());
                Log.debug("  args[{}].required : {}", i, arg.required());
                Log.debug("  args[{}].type     : {}", i, arg.type());
            }
        }

        // Commands with resolved values
        Log.debug("--- Commands ({}) ---", script.commands().size());
        List<CommandStep> commands = script.commands();
        if (commands.isEmpty()) {
            Log.debug("  <no commands>");
        } else {
            for (int i = 0; i < commands.size(); i++) {
                CommandStep cmd = commands.get(i);
                Log.debug("  commands[{}].command : '{}'", i, cmd.command());
                
                // Print resolved values for this command
                printResolvedCommand(i, cmd, def, sourceName);
            }
        }

        Log.debug("=== END SCRIPT DEBUG: {} ===", sourceName);
    }

    private static void printResolvedCommand(int index, CommandStep cmd, Defaults defaults, String sourceName) {
        Log.debug("  --- Resolved Values for commands[{}] ---", index);
        
        // Start with defaults
        Target defaultTarget = defaults.target();
        Duration defaultDelay = defaults.delay();
        Duration defaultTimeout = defaultTarget.server().timeout();

        // Apply overrides
        CommandOverrides overrides = cmd.overrides();
        
        // Resolved target values
        Target resolvedTarget = defaultTarget;
        if (overrides != null && overrides.target() != null) {
            resolvedTarget = mergeTarget(defaultTarget, overrides.target());
        }
        
        printTarget(resolvedTarget, "  commands[" + index + "].resolved");

        // Resolved timing values
        Duration resolvedDelay = defaultDelay;
        Duration resolvedTimeout = defaultTimeout;
        
        if (overrides != null) {
            if (overrides.delay() != null) {
                resolvedDelay = overrides.delay();
            }
            if (overrides.timeout() != null) {
                resolvedTimeout = overrides.timeout();
            }
        }
        
        Log.debug("  commands[{}].resolved.delay   : {} {}", index, formatDuration(resolvedDelay), 
                  getOverrideInfo(overrides != null ? overrides.delay() : null, "from override", "from defaults"));
        Log.debug("  commands[{}].resolved.timeout : {} {}", index, formatDuration(resolvedTimeout),
                  getOverrideInfo(overrides != null ? overrides.timeout() : null, "from override", "from target.server.timeout"));
    }

    private static Target mergeTarget(Target base, Target override) {
        // Merge target values, with override taking precedence
        Target.RunAs runAs = override.runAs() != null ? override.runAs() : base.runAs();
        String id = override.id() != null ? override.id() : base.id();
        
        TargetKind kind = mergeTargetKind(base.kind(), override.kind());
        TargetServer server = mergeTargetServer(base.server(), override.server());
        
        return new Target(runAs, id, kind, server);
    }

    private static TargetKind mergeTargetKind(TargetKind base, TargetKind override) {
        if (override == null) return base;
        
        TargetKind.Type register = override.register() != null ? override.register() : base.register();
        TargetKind.Type execute = override.execute() != null ? override.execute() : base.execute();
        
        return new TargetKind(register, execute);
    }

    private static TargetServer mergeTargetServer(TargetServer base, TargetServer override) {
        if (override == null) return base;
        
        boolean targetRequired = override.targetRequired();
        boolean scheduleOnline = override.scheduleOnline();
        Duration timeout = override.timeout() != null ? override.timeout() : base.timeout();
        Duration frequency = override.frequency() != null ? override.frequency() : base.frequency();
        
        return new TargetServer(targetRequired, scheduleOnline, timeout, frequency);
    }

    private static void printTarget(Target target, String prefix) {
        Log.debug("{}.runAs              : {}", prefix, target.runAs());
        Log.debug("{}.id                 : '{}'", prefix, target.id());
        
        TargetKind kind = target.kind();
        Log.debug("{}.kind.register      : {}", prefix, kind.register());
        Log.debug("{}.kind.execute       : {}", prefix, kind.execute());
        
        TargetServer server = target.server();
        Log.debug("{}.server.targetRequired : {}", prefix, server.targetRequired());
        Log.debug("{}.server.scheduleOnline : {}", prefix, server.scheduleOnline());
        Log.debug("{}.server.timeout        : {}", prefix, formatDuration(server.timeout()));
        Log.debug("{}.server.frequency      : {}", prefix, formatDuration(server.frequency()));
    }

    private static String formatDuration(Duration duration) {
        if (duration == null) {
            return "<null>";
        }
        if (duration.isZero()) {
            return "0s";
        }
        
        long totalSeconds = duration.getSeconds();
        long nanos = duration.getNano();
        
        if (totalSeconds == 0 && nanos > 0) {
            return nanos / 1_000_000 + "ms";
        }
        
        if (totalSeconds < 60) {
            return totalSeconds + "s";
        }
        
        long minutes = totalSeconds / 60;
        long remainingSeconds = totalSeconds % 60;
        
        if (minutes < 60) {
            return remainingSeconds == 0 
                ? minutes + "m" 
                : minutes + "m " + remainingSeconds + "s";
        }
        
        long hours = minutes / 60;
        long remainingMinutes = minutes % 60;
        
        StringBuilder sb = new StringBuilder();
        sb.append(hours).append("h");
        if (remainingMinutes > 0) {
            sb.append(" ").append(remainingMinutes).append("m");
        }
        if (remainingSeconds > 0) {
            sb.append(" ").append(remainingSeconds).append("s");
        }
        
        return sb.toString();
    }

    private static String formatStringList(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "<empty>";
        }
        return "['" + String.join("', '", list) + "']";
    }

    private static String getOverrideInfo(Object overrideValue, String overrideText, String defaultText) {
        return overrideValue != null ? "(" + overrideText + ")" : "(" + defaultText + ")";
    }
}
