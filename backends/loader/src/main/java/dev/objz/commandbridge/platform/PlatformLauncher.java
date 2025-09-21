package dev.objz.commandbridge.platform;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import dev.objz.commandbridge.main.logging.Log;

public final class PlatformLauncher {
	private static final AtomicReference<PlatformAdapter> ADAPTER = new AtomicReference<>();

	private PlatformLauncher() {
	}

	public static void start(PlatformAdapter.PlatformEnv env) {
		if (ADAPTER.get() != null)
			return;
		Optional<PlatformAdapter> loaded = PlatformDetector.loadAdapter();
		if (loaded.isEmpty()) {
			Log.error("No platform adapter found for this runtime");
			throw new IllegalStateException("No adapter");
		}
		try {
			loaded.get().start(env);
			ADAPTER.set(loaded.get());
		} catch (Exception e) {
			Log.error("Failed to start adapter", e);
			throw new RuntimeException(e);
		}
	}

	public static void stop(PlatformAdapter.PlatformEnv env) {
		PlatformAdapter a = ADAPTER.getAndSet(null);
		if (a == null)
			return;
		try {
			a.stop();
		} catch (Exception e) {
			Log.error("Error while stopping adapter", e);
		}
	}
}
