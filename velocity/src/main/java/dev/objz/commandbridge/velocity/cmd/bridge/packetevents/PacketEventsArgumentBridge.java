package dev.objz.commandbridge.velocity.cmd.bridge.packetevents;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import dev.objz.commandbridge.velocity.cmd.bridge.framework.CustomArgumentRegistry;
import java.util.Objects;

public final class PacketEventsArgumentBridge {
    private final DeclareCommandsPatchListener patchListener;

    public PacketEventsArgumentBridge(CustomArgumentRegistry registry) {
        Objects.requireNonNull(registry, "registry");
        this.patchListener = new DeclareCommandsPatchListener(registry);
    }

    public void install() {
        PacketEvents.getAPI().getEventManager().registerListener(patchListener,
                PacketListenerPriority.NORMAL);
    }
}
