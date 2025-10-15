package dev.objz.commandbridge.velocity.registry;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.objz.commandbridge.config.model.VelocityConfig;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.logging.StatusLog;
import dev.objz.commandbridge.proto.Envelope;
import dev.objz.commandbridge.proto.MessageType;
import dev.objz.commandbridge.proto.cmd.CommandStub;
import dev.objz.commandbridge.proto.cmd.RegisterCommandsPayload;
import dev.objz.commandbridge.velocity.ws.ClientSession;
import dev.objz.commandbridge.velocity.ws.SessionHub;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public final class OnAuthRegisterCommands {
	private OnAuthRegisterCommands() {
	}

	public static void install(SessionHub sessions,
			ScriptManager mgr,
			ObjectMapper mapper,
			String serverId,
			VelocityConfig config) {

		sessions.onAuthed((ClientSession s) -> {
			String clientId = s.clientId();

			List<CommandStub> stubs = StubExporter.exportForBackend(mgr.enabled(), clientId);

			if (stubs.isEmpty()) {
				Log.info("No commands to register for backend '{}'", clientId);
				return;
			}

			RegisterCommandsPayload payload = new RegisterCommandsPayload(true, stubs);
			Envelope env = Envelope.make(
					MessageType.REGISTER_COMMANDS,
					serverId,
					clientId,
					mapper.valueToTree(payload));

			sessions.send(s.ch(), env);
			StatusLog.registerPushed(stubs.size(), clientId);

			CompletableFuture<Envelope> fut = new CompletableFuture<>();
			sessions.expectFeedback(env.id().toString(), clientId, fut);
			fut.orTimeout(config.timeouts().registerTimeout(), TimeUnit.SECONDS)
					.exceptionally(ex -> {
						Log.error("Register feedback timeout from '{}' (envelope-id={})",
								clientId, env.id());
						return null;
					});
		});
	}
}
