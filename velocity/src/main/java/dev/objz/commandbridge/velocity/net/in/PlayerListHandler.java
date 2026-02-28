package dev.objz.commandbridge.velocity.net.in;

import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.net.Endpoint;
import dev.objz.commandbridge.net.InboundHandler;
import dev.objz.commandbridge.net.payloads.util.PlayerListPayload;
import dev.objz.commandbridge.net.proto.Envelope;
import dev.objz.commandbridge.velocity.net.session.ClientSession;
import dev.objz.commandbridge.velocity.net.session.SessionHub;
import dev.objz.commandbridge.velocity.util.PlayerTracker;

import java.util.Objects;

public final class PlayerListHandler extends InboundHandler {

    private final SessionHub sessions;
    private final PlayerTracker playerTracker;

    public PlayerListHandler(SessionHub sessions, PlayerTracker playerTracker) {
        this.sessions = Objects.requireNonNull(sessions);
        this.playerTracker = Objects.requireNonNull(playerTracker);
    }

    @Override
    public void accept(Endpoint endpoint, Envelope env) {
        if (env.payload() == null) {
            Log.warn("Received PLAYER_LIST with null payload from '{}'", env.from());
            return;
        }

        PlayerListPayload payload;
        try {
            payload = Envelope.MAPPER.treeToValue(env.payload(), PlayerListPayload.class);
        } catch (Exception e) {
            Log.error(e, "Failed to parse PLAYER_LIST from '{}'", env.from());
            return;
        }

        if (payload == null || payload.players() == null) {
            Log.warn("Parsed PLAYER_LIST is null from '{}'", env.from());
            return;
        }

        ClientSession session = sessions.get(endpoint);
        if (session == null) {
            Log.warn("No session found for PLAYER_LIST from '{}'", env.from());
            return;
        }

        playerTracker.update(session.id(), payload.players());
        Log.debug("Updated player list for '{}': {} players", session.id(), payload.players().size());
    }
}
