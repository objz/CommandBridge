package dev.objz.commandbridge.velocity;

import com.velocitypowered.api.proxy.ProxyServer;
import dev.objz.commandbridge.config.model.VelocityConfig;
import dev.objz.commandbridge.net.Endpoint;
import dev.objz.commandbridge.net.OutNode;
import dev.objz.commandbridge.net.ResponseAwaiter;
import dev.objz.commandbridge.net.SendOperation;
import dev.objz.commandbridge.net.proto.Envelope;
import dev.objz.commandbridge.net.proto.MessageType;
import dev.objz.commandbridge.scripting.model.enums.Location;
import dev.objz.commandbridge.velocity.cmd.bridge.framework.CustomArgumentRegistry;
import dev.objz.commandbridge.velocity.net.out.RegistrationRequest;
import dev.objz.commandbridge.velocity.net.session.ClientSession;
import dev.objz.commandbridge.velocity.net.session.SessionHub;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RegistrationManagerSyncTest {

    @BeforeAll
    static void setUp() {
        TestFixtures.ensureLog();
    }

    @Test
    void aServerWithoutScriptsIsToldSoItDropsOldCommands() {
        List<Envelope> sent = new ArrayList<>();
        OutNode out = new OutNode();
        out.setServerId("proxy-1");
        out.setEndpointSendFactory((endpoint, env) -> {
            sent.add(env);
            return new SendOperation(endpoint, env, new ResponseAwaiter());
        });
        out.register(MessageType.REGISTER_COMMANDS, new RegistrationRequest());
        ProxyServer proxy = (ProxyServer) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { ProxyServer.class }, (p, m, a) -> null);
        var manager = new RegistrationManager(proxy, new SessionHub(), VelocityConfig.defaults(), out, new CustomArgumentRegistry());

        Endpoint lobbyEndpoint = new Endpoint() {
            @Override
            public CompletableFuture<Void> send(Envelope env) {
                return CompletableFuture.completedFuture(null);
            }

            @Override
            public boolean isOpen() {
                return true;
            }
        };
        ClientSession lobby = new ClientSession(lobbyEndpoint, "lobby");
        lobby.location(Location.BACKEND);

        manager.onClientAuthenticated(lobby);

        assertEquals(1, sent.size());
        assertEquals(MessageType.REGISTER_COMMANDS, sent.get(0).type());
        assertEquals(0, sent.get(0).payload().path("commands").size());
    }
}
