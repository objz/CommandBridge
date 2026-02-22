package dev.objz.commandbridge.scripting.bind;

import dev.objz.commandbridge.scripting.platform.PlatformFeatures;
import dev.objz.commandbridge.scripting.validation.ProblemSink;

public record BindContext(TypeAdapterRegistry adapters, ProblemSink problems,
        PlatformFeatures platformFeatures) implements ConvertContext {
    public BindContext {
        if (platformFeatures == null) {
            platformFeatures = PlatformFeatures.none();
        }
    }
}
