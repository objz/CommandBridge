package dev.objz.commandbridge.api.message;

import dev.objz.commandbridge.api.platform.Platform;

@FunctionalInterface
public interface ServerEventListener {
    void accept(Platform.ServerTarget server);
}
