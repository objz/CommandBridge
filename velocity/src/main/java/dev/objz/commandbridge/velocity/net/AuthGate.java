package dev.objz.commandbridge.velocity.net;

import dev.objz.commandbridge.net.Endpoint;
import dev.objz.commandbridge.net.proto.Envelope;
import dev.objz.commandbridge.net.proto.MessageType;
import dev.objz.commandbridge.security.AuthStatus;
import dev.objz.commandbridge.velocity.net.session.ClientSession;
import dev.objz.commandbridge.velocity.net.session.SessionHub;

/** The proxy only listens to a connection once it authenticated; until then it may only ask to. */
public final class AuthGate {

    private AuthGate() {
    }

    public static boolean allows(SessionHub sessions, Endpoint endpoint, Envelope env) {
        if (env.type() == MessageType.AUTH_REQUEST)
            return true;
        ClientSession session = sessions.get(endpoint);
        return session != null && session.status() == AuthStatus.AUTH_OK;
    }
}
