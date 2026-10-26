package dev.objz.commandbridge.backends.net.in;

import dev.objz.commandbridge.net.Endpoint;
import dev.objz.commandbridge.net.InboundHandler;
import dev.objz.commandbridge.net.ResponseAwaiter;
import dev.objz.commandbridge.net.SendOperation;
import dev.objz.commandbridge.net.proto.Envelope;
import dev.objz.commandbridge.net.proto.MessageType;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Endpoint that records replies sent by an inbound handler wired to it. */
final class RecordingEndpoint implements Endpoint {

    final List<Envelope> sent = new CopyOnWriteArrayList<>();

    RecordingEndpoint wire(InboundHandler handler) {
        handler.setSendOperationFactory((ep, env) -> new SendOperation(ep, env, new ResponseAwaiter()));
        return this;
    }

    @Override
    public CompletableFuture<Void> send(Envelope env) {
        sent.add(env);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public boolean isOpen() {
        return true;
    }

    <T> T onlyReply(MessageType type, Class<T> payloadType) throws Exception {
        assertEquals(1, sent.size(), "expected exactly one reply");
        Envelope reply = sent.getFirst();
        assertEquals(type, reply.type());
        return Envelope.MAPPER.treeToValue(reply.payload(), payloadType);
    }
}
