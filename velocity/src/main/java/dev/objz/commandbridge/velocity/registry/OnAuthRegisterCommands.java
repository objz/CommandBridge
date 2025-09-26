package dev.objz.commandbridge.velocity.registry;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.objz.commandbridge.main.config.model.VelocityConfig;
import dev.objz.commandbridge.main.logging.Log;
import dev.objz.commandbridge.main.logging.StatusLog;
import dev.objz.commandbridge.main.proto.Envelope;
import dev.objz.commandbridge.main.proto.MessageType;
import dev.objz.commandbridge.main.proto.cmd.CommandStub;
import dev.objz.commandbridge.main.proto.cmd.RegisterCommandsPayload;
import dev.objz.commandbridge.velocity.scripting.ScriptManager;
import dev.objz.commandbridge.velocity.ws.ClientSession;
import dev.objz.commandbridge.velocity.ws.SessionHub;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public final class OnAuthRegisterCommands {
	private OnAuthRegisterCommands() {
	}

	public static void install(SessionHub sessions, ScriptManager mgr, ObjectMapper mapper, String serverId, VelocityConfig config) {
		sessions.onAuthed((ClientSession s) -> {
			List<CommandStub> stubs = StubExporter.export(mgr.enabled());
			RegisterCommandsPayload payload = new RegisterCommandsPayload(true, stubs);
			Envelope env = Envelope.make(MessageType.REGISTER_COMMANDS, serverId, s.clientId(),
					mapper.valueToTree(payload));

			sessions.send(s.ch(), env);
			StatusLog.registerPushed(stubs.size(), s.clientId());

			CompletableFuture<Envelope> fut = new CompletableFuture<>();
			sessions.expectFeedback(env.id().toString(), s.clientId(), fut);
			fut.orTimeout(config.timeouts().registerTimeout(), TimeUnit.SECONDS).exceptionally(ex -> {
				Log.error(
						"Register feedback timeout from '{}' (envelope-id={})",
						s.clientId(), env.id());
				return null;
			});
		});
	}
}
