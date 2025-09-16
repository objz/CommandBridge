package dev.objz.commandbridge.velocity.registry;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.objz.commandbridge.main.logging.Log;
import dev.objz.commandbridge.main.proto.Envelope;
import dev.objz.commandbridge.main.proto.MessageType;
import dev.objz.commandbridge.main.proto.cmd.CommandStub;
import dev.objz.commandbridge.main.proto.cmd.RegisterCommandsPayload;
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
			RegisterCommandsPayload payload = new RegisterCommandsPayload(true, stubs);
			Envelope env = Envelope.make(MessageType.REGISTER_COMMANDS, serverId, s.clientId(),
					mapper.valueToTree(payload));
			sessions.send(s.ch(), env);
			final int n = stubs.size();
			final String msg = String.format(
					"%sRegister:%s pushed %s%d%s command stub%s to %s'%s'%s",
					Log.GRAY, Log.RESET,
					(n > 0 ? Log.GREEN : Log.GRAY), n, Log.RESET, (n == 1 ? "" : "s"),
					Log.GRAY, s.clientId(), Log.RESET);
			Log.info(msg);
		});
	}
}
