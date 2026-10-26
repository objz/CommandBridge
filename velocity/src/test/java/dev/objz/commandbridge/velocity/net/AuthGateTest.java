package dev.objz.commandbridge.velocity.net;

import dev.objz.commandbridge.net.Endpoint;
import dev.objz.commandbridge.net.proto.Envelope;
import dev.objz.commandbridge.net.proto.MessageType;
import dev.objz.commandbridge.security.AuthStatus;
import dev.objz.commandbridge.velocity.net.session.SessionHub;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthGateTest {

    private static Endpoint endpoint() {
        return new Endpoint() {
            @Override
            public CompletableFuture<Void> send(Envelope env) {
                return CompletableFuture.completedFuture(null);
            }

            @Override
            public boolean isOpen() {
                return true;
            }
        };
    }

    private static Envelope env(MessageType type) {
        return Envelope.make(type, "lobby", "proxy", null);
    }

    @Test
    void aStrangerMayOnlyAuthenticate() {
        SessionHub sessions = new SessionHub();
        Endpoint stranger = endpoint();

        assertTrue(AuthGate.allows(sessions, stranger, env(MessageType.AUTH_REQUEST)));
        assertFalse(AuthGate.allows(sessions, stranger, env(MessageType.PLUGIN_MESSAGE)));
        assertFalse(AuthGate.allows(sessions, stranger, env(MessageType.EXECUTE_COMMAND_RESULT)));
    }

    @Test
    void aSessionThatIsNotAuthenticatedYetIsHeldBack() {
        SessionHub sessions = new SessionHub();
        Endpoint pending = endpoint();
        sessions.add("lobby", pending).status(AuthStatus.AUTH_FAIL);

        assertFalse(AuthGate.allows(sessions, pending, env(MessageType.PLUGIN_MESSAGE)));
    }

    @Test
    void anAuthenticatedServerGetsThrough() {
        SessionHub sessions = new SessionHub();
        Endpoint lobby = endpoint();
        sessions.add("lobby", lobby).status(AuthStatus.AUTH_OK);

        assertTrue(AuthGate.allows(sessions, lobby, env(MessageType.PLUGIN_MESSAGE)));
    }
}
