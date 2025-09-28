package dev.objz.commandbridge.main.scripting.v3.api.groups.args;

import java.util.Set;

import dev.objz.commandbridge.main.scripting.v3.api.groups.Arg;
import dev.objz.commandbridge.main.scripting.v3.api.groups.defaults.TargetKind;


public record BasicArg(
        String name,
        boolean required,
        ArgType type,
        Set<TargetKind.Type> allowedRegisters
) implements Arg {
    
    public static BasicArg withDefaults(String name, boolean required, ArgType type) {
        return new BasicArg(name, required, type, type.getDefaultAllowedRegisters());
    }
    
    public static BasicArg backendOnly(String name, boolean required, ArgType type) {
        return new BasicArg(name, required, type, Set.of(TargetKind.Type.BACKEND));
    }
    
    public static BasicArg velocityOnly(String name, boolean required, ArgType type) {
        return new BasicArg(name, required, type, Set.of(TargetKind.Type.VELOCITY));
    }
    
    public static BasicArg both(String name, boolean required, ArgType type) {
        return new BasicArg(name, required, type, Set.of(TargetKind.Type.BACKEND, TargetKind.Type.VELOCITY));
    }
}
