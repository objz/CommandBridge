package dev.objz.commandbridge.api.message;

import dev.objz.commandbridge.api.platform.Platform;

/** Listener for server lifecycle events in the bridge network. */
@FunctionalInterface
public interface ServerEventListener {
    /**
     * Invoked when a server connects or disconnects.
     *
     * @param server the server target involved in the event
     */
    void accept(Platform.ServerTarget server);
}
