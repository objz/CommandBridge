package dev.objz.commandbridge.velocity.net.in;

import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.net.Endpoint;
import dev.objz.commandbridge.net.InboundHandler;
import dev.objz.commandbridge.net.payloads.util.PlayerUpdatePayload;
import dev.objz.commandbridge.net.proto.Envelope;
import dev.objz.commandbridge.net.proto.MessageType;
import dev.objz.commandbridge.velocity.net.session.ClientSession;
import dev.objz.commandbridge.velocity.net.session.SessionHub;
import dev.objz.commandbridge.velocity.util.PlayerTracker;

import java.util.Objects;

public final class PlayerUpdateHandler extends InboundHandler {

    private final SessionHub sessions;
    private final PlayerTracker playerTracker;

    public PlayerUpdateHandler(SessionHub sessions, PlayerTracker playerTracker) {
        this.sessions = Objects.requireNonNull(sessions);
        this.playerTracker = Objects.requireNonNull(playerTracker);
    }

    @Override
    public void accept(Endpoint endpoint, Envelope env) {
        if (env.payload() == null) {
            Log.warn("Received {} with null payload from '{}'", env.type(), env.from());
            return;
        }

        PlayerUpdatePayload payload;
        try {
            payload = Envelope.MAPPER.treeToValue(env.payload(), PlayerUpdatePayload.class);
        } catch (Exception e) {
            Log.error(e, "Failed to parse {} from '{}'", env.type(), env.from());
            return;
        }

        if (payload == null || payload.player() == null) {
            Log.warn("Parsed {} has null player from '{}'", env.type(), env.from());
            return;
        }

        ClientSession session = sessions.get(endpoint);
        if (session == null) {
            Log.warn("No session found for {} from '{}'", env.type(), env.from());
            return;
        }

        if (env.type() == MessageType.PLAYER_JOIN) {
            playerTracker.addPlayer(session.id(), payload.player());
            Log.debug("Player joined on '{}': {}", session.id(), payload.player());
        } else if (env.type() == MessageType.PLAYER_LEAVE) {
            playerTracker.removePlayer(session.id(), payload.player());
            Log.debug("Player left on '{}': {}", session.id(), payload.player());
        }
    }
}
