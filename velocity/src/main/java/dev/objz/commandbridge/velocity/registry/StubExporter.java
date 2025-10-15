package dev.objz.commandbridge.velocity.registry;

import dev.objz.commandbridge.proto.cmd.CommandStub;
import dev.objz.commandbridge.scripting.model.Script;
import dev.objz.commandbridge.scripting.model.enums.Location;
import dev.objz.commandbridge.scripting.model.records.mapping.ArgMapping;
import dev.objz.commandbridge.scripting.model.records.mapping.IdMapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class StubExporter {
	private StubExporter() {
	}

	public static List<CommandStub> export(List<Script> scripts) {
		List<CommandStub> out = new ArrayList<>();
		if (scripts == null || scripts.isEmpty())
			return out;

		for (Script s : scripts) {
			List<ArgMapping> usedArgs = s.usedArguments();

			out.add(new CommandStub(
					s.name(),
					s.aliases(),
					s.description(),
					usedArgs));
		}
		return out;
	}

	public static List<CommandStub> exportForBackend(List<Script> scripts, String clientId) {
		List<CommandStub> out = new ArrayList<>();
		if (scripts == null || scripts.isEmpty() || clientId == null || clientId.isBlank()) {
			return out;
		}

		for (Script s : scripts) {
			if (!shouldRegisterOnBackend(s, clientId)) {
				continue;
			}

			List<ArgMapping> usedArgs = s.usedArguments();

			out.add(new CommandStub(
					s.name(),
					s.aliases(),
					s.description(),
					usedArgs));
		}
		return out;
	}

	private static boolean shouldRegisterOnBackend(Script script, String clientId) {
		if (script.register() == null || script.register().isEmpty()) {
			return false;
		}

		for (IdMapping mapping : script.register()) {
			if (mapping != null
					&& mapping.location() == Location.BACKEND
					&& clientId.equals(mapping.id())) {
				return true;
			}
		}

		return false;
	}

}
