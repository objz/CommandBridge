package dev.objz.commandbridge.velocity.cmd.bridge.framework;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import java.util.Objects;

public final class ArgumentBridge {
	private final CustomArgumentRegistry registry;
	private final DeclareCommandsPatchListener patchListener;

	public ArgumentBridge() {
		this(new CustomArgumentRegistry());
	}

	public ArgumentBridge(CustomArgumentRegistry registry) {
		this.registry = Objects.requireNonNull(registry);
		this.patchListener = new DeclareCommandsPatchListener(registry);
	}

	public CustomArgumentRegistry registry() {
		return registry;
	}

	public void install() {
		PacketEvents.getAPI().getEventManager().registerListener(patchListener,
				PacketListenerPriority.NORMAL);
	}
}
