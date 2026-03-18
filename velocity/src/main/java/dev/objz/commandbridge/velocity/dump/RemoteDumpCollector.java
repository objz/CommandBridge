package dev.objz.commandbridge.velocity.dump;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.objz.commandbridge.net.OutNode;
import dev.objz.commandbridge.net.SendOperation;
import dev.objz.commandbridge.net.proto.Envelope;
import dev.objz.commandbridge.net.proto.MessageType;
import dev.objz.commandbridge.security.AuthStatus;
import dev.objz.commandbridge.velocity.net.out.ctx.DumpRequestContext;
import dev.objz.commandbridge.velocity.net.session.ClientSession;
import dev.objz.commandbridge.velocity.net.session.SessionHub;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class RemoteDumpCollector {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final SessionHub sessions;
    private final OutNode outNode;
    private final Duration timeout;

    public RemoteDumpCollector(SessionHub sessions, OutNode outNode, Duration timeout) {
        this.sessions = sessions;
        this.outNode = outNode;
        this.timeout = timeout;
    }

    public List<JsonNode> collect() {
        List<CompletableFuture<ObjectNode>> futures = new ArrayList<>();

        for (ClientSession session : sessions) {
            ObjectNode base = baseNode(session);

            if (session.status() != AuthStatus.AUTH_OK || session.endpoint() == null || !session.endpoint().isOpen()) {
                base.put("collected", false);
                base.put("reason", "client not authenticated or endpoint closed");
                futures.add(CompletableFuture.completedFuture(base));
                continue;
            }

            try {
                SendOperation operation = outNode.send(
                        MessageType.DUMP_REQUEST,
                        new DumpRequestContext(session, timeout));

                CompletableFuture<ObjectNode> one = operation.await()
                        .handle((Envelope env, Throwable ex) -> {
                            if (ex != null) {
                                base.put("collected", false);
                                base.put("reason", safeMessage(ex));
                                return base;
                            }

                            base.put("collected", true);
                            base.set("snapshot", DumpSanitizer.sanitize(env.payload()));
                            return base;
                        });
                futures.add(one);
            } catch (Exception ex) {
                base.put("collected", false);
                base.put("reason", safeMessage(ex));
                futures.add(CompletableFuture.completedFuture(base));
            }
        }

        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();

        List<JsonNode> snapshots = new ArrayList<>();
        for (CompletableFuture<ObjectNode> future : futures) {
            try {
                snapshots.add(future.join());
            } catch (Exception ex) {
                ObjectNode fallback = MAPPER.createObjectNode();
                fallback.put("collected", false);
                fallback.put("reason", safeMessage(ex));
                snapshots.add(fallback);
            }
        }

        return snapshots;
    }

    private static ObjectNode baseNode(ClientSession session) {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("id", session.id() != null ? session.id() : "");
        node.put("location", session.location() != null ? session.location().name() : "UNKNOWN");
        node.put("status", session.status() != null ? session.status().name() : "UNKNOWN");
        node.put("endpoint", session.endpoint() != null ? session.endpoint().describe() : "unknown");
        node.put("connected", session.endpoint() != null && session.endpoint().isOpen());
        return node;
    }

    private static String safeMessage(Throwable throwable) {
        if (throwable == null) {
            return "unknown error";
        }
        Throwable cause = throwable.getCause() != null ? throwable.getCause() : throwable;
        if (cause.getMessage() == null || cause.getMessage().isBlank()) {
            return cause.getClass().getSimpleName();
        }
        return cause.getMessage();
    }
}
