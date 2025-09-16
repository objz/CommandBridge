package dev.objz.commandbridge.backends.api;

import dev.objz.commandbridge.main.proto.cmd.CommandStub;

import java.util.List;

public interface PlatformRegistry {
	RegistrationResult registerAll(boolean reload, List<CommandStub> stubs);

	void unregisterAll();

	record RegistrationResult(int requested, int registered, int failed, List<String> warnings,
			List<String> errors) {
		public static RegistrationResult empty() {
			return new RegistrationResult(0, 0, 0, List.of(), List.of());
		}
	}
}
