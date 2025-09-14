package dev.objz.commandbridge.velocity.registry;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import dev.objz.commandbridge.main.logging.Log;
import dev.objz.commandbridge.main.proto.Envelope;
import dev.objz.commandbridge.main.proto.MessageType;
import dev.objz.commandbridge.main.proto.cmd.CommandStub;
import dev.objz.commandbridge.velocity.scripting.ScriptManager;
import dev.objz.commandbridge.velocity.ws.ClientSession;
import dev.objz.commandbridge.velocity.ws.SessionHub;

import java.util.List;

public final class OnAuthRegisterCommands {
	private OnAuthRegisterCommands() {
	}

	/**
	 * Registers a listener on the provided SessionHub. When a backend
	 * authenticates, this listener will export stubs from the given
	 * ScriptManager, convert them into JSON, and send a REGISTER_COMMANDS
	 * message to that backend.
	 */
	public static void install(SessionHub sessions, ScriptManager mgr, ObjectMapper mapper, String serverId) {
		sessions.onAuthed((ClientSession s) -> {
			List<CommandStub> stubs = StubExporter.export(mgr);
			ArrayNode payload = mapper.valueToTree(stubs);
			Envelope env = Envelope.make(MessageType.REGISTER_COMMANDS, serverId, s.clientId(), payload);
			sessions.send(s.ch(), env);
			Log.success("Pushed {} command stub(s) to {}", stubs.size(), s.clientId());
		});
	}
}
