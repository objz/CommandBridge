package dev.objz.commandbridge.backends;

import dev.objz.commandbridge.backends.debug.CommandRegistrationDebug;
import dev.objz.commandbridge.main.logging.Log;
import dev.objz.commandbridge.main.proto.cmd.CommandStub;
import dev.objz.commandbridge.main.proto.feedback.Feedback;
import dev.objz.commandbridge.main.proto.feedback.FeedbackCollector;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Unified base for platform registries (Bukkit/Folia).
 * - Implements reload logic
 * - Counts with FeedbackCollector
 * - Optional deep debug dump
 */
public abstract class PlatformRegistry {

	protected final List<CommandStub> installed = new CopyOnWriteArrayList<>();

	public final Feedback registerAll(boolean reload, List<CommandStub> stubs) {
		if (reload) {
			try {
				unregisterAll();
			} catch (Throwable t) {
				Log.warn("{} unregister during reload reported: {}", platformName(), t.toString());
			}
		}
		if (stubs == null || stubs.isEmpty()) {
			return Feedback.empty();
		}

		if (Log.isDebug())
			CommandRegistrationDebug.dumpStubs(stubs);

		FeedbackCollector fc = new FeedbackCollector();
		for (CommandStub s : stubs) {
			try {
				doRegister(s); // platform-specific
				installed.add(s);
				fc.success();
			} catch (Throwable t) {
				fc.failure("Failed: " + s.name() + " -> " + t.getMessage());
			}
		}
		return fc.build();
	}

	public final void unregisterAll() {
		try {
			doUnregisterAll();
		} catch (Exception e) {
			Log.warn("{} unregisterAll reported: {}", platformName(), e.toString());
		} finally {
			installed.clear();
		}
	}

	/** Human-friendly platform name for logs. */
	protected abstract String platformName();

	/** Hook: perform the actual registration against the platform’s command API. */
	protected abstract void doRegister(CommandStub stub) throws Exception;

	/**
	 * Hook: perform platform-wide deregistration of commands previously registered.
	 */
	protected abstract void doUnregisterAll() throws Exception;
}
