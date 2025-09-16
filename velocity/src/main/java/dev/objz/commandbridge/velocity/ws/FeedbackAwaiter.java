package dev.objz.commandbridge.velocity.ws;

import dev.objz.commandbridge.main.logging.Log;
import dev.objz.commandbridge.main.proto.Envelope;
import dev.objz.commandbridge.main.proto.MessageType;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.*;

final class FeedbackAwaiter {
	private final ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
	private final Map<String, CompletableFuture<Envelope>> waiters = new ConcurrentHashMap<>();

	void expect(String envelopeId, MessageType resultType, Duration timeout, String opName, String backendId) {
		CompletableFuture<Envelope> cf = new CompletableFuture<>();
		waiters.put(key(envelopeId, resultType), cf);

		exec.schedule(() -> {
			if (!cf.isDone()) {
				waiters.remove(key(envelopeId, resultType));
				Log.error("{} feedback timeout from '{}' (envelope-id={})", opName, backendId,
						envelopeId);
			}
		}, timeout.toMillis(), TimeUnit.MILLISECONDS);
	}

	void complete(Envelope env) {
		var cf = waiters.remove(key(env.id().toString(), env.type()));
		if (cf != null)
			cf.complete(env);
	}

	void shutdown() {
		exec.shutdownNow();
		waiters.clear();
	}

	private static String key(String id, MessageType t) {
		return id + "|" + t.name();
	}
}
