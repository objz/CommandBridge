
package dev.objz.commandbridge.backends.api;

import dev.objz.commandbridge.backends.debug.CommandRegistrationDebug;
import dev.objz.commandbridge.main.logging.Log;
import dev.objz.commandbridge.main.proto.cmd.CommandStub;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

//Shared template for platform registries.
public abstract class AbstractCommandRegistry implements PlatformRegistry {

	protected final List<CommandStub> installed = new CopyOnWriteArrayList<>();

	@Override
	public final RegistrationResult registerAll(boolean reload, List<CommandStub> stubs) {
		if (reload) {
			try {
				unregisterAll();
			} catch (Throwable t) {
				Log.warn("{} unregister during reload reported: {}", platformName(), t.toString());
			}
		}

		if (stubs == null || stubs.isEmpty()) {
			return RegistrationResult.empty();
		}

		CommandRegistrationDebug.dumpStubs(stubs);

		final int requested = stubs.size();
		int registered = 0;
		int failed = 0;
		List<String> warnings = new ArrayList<>();
		List<String> errors = new ArrayList<>();

		for (CommandStub s : stubs) {
			try {
				doRegister(s);
				installed.add(s);
				registered++;
			} catch (Throwable t) {
				errors.add("Failed: " + s.name() + " -> " + t.getMessage());
				failed++;
			}
		}

		RegistrationResult result = new RegistrationResult(requested, registered, failed, warnings, errors);
		CommandRegistrationDebug.dumpResult(result);
		return result;
	}

	@Override
	public final void unregisterAll() {
		try {
			doUnregisterAll();
		} catch (Exception e) {
			Log.warn("{} unregisterAll reported: {}", platformName(), e.toString());
		} finally {
			installed.clear();
		}
	}

	protected abstract String platformName();

	protected abstract void doRegister(CommandStub stub) throws Exception;

	protected abstract void doUnregisterAll() throws Exception;
}
