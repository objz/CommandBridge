package dev.objz.commandbridge.velocity.cli.subcommands;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.objz.commandbridge.config.ConfigManager;
import dev.objz.commandbridge.config.model.VelocityConfig;
import dev.objz.commandbridge.net.Endpoint;
import dev.objz.commandbridge.net.OutNode;
import dev.objz.commandbridge.net.OutboundHandler;
import dev.objz.commandbridge.net.SendOperation;
import dev.objz.commandbridge.net.proto.Envelope;
import dev.objz.commandbridge.net.proto.MessageType;
import dev.objz.commandbridge.scripting.model.Script;
import dev.objz.commandbridge.scripting.model.enums.Location;
import dev.objz.commandbridge.scripting.model.records.mapping.IdMapping;
import dev.objz.commandbridge.security.AuthStatus;
import dev.objz.commandbridge.velocity.RegistrationManager;
import dev.objz.commandbridge.velocity.ScriptManager;
import dev.objz.commandbridge.velocity.TestFixtures;
import dev.objz.commandbridge.velocity.cmd.bridge.framework.CustomArgumentRegistry;
import dev.objz.commandbridge.velocity.net.out.ctx.RegistrationRequestContext;
import dev.objz.commandbridge.velocity.net.session.SessionHub;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ReloadCommandTest {
    @TempDir
    Path dir;

    @BeforeAll
    static void setup() {
        TestFixtures.ensureLog();
    }

    @Test
    void removingTheLastScriptSynchronizesEveryAuthenticatedClientAndWaitsForEveryResult() {
        checkReload(false);
    }

    @Test
    void aFailureCallbackFollowedByASendExceptionCountsOnlyOnce() {
        checkReload(true);
    }

    private void checkReload(boolean callbackThenThrow) {
        List<Component> messages = new ArrayList<>();
        Player sender = (Player) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { Player.class },
                (p, method, args) -> {
                    if (method.getName().equals("sendMessage") && args[0] instanceof Component message)
                        messages.add(message);
                    return null;
                });
        SessionHub hub = new SessionHub();
        hub.add("lobby", endpoint()).status(AuthStatus.AUTH_OK);
        hub.add("empty", endpoint()).status(AuthStatus.AUTH_OK);
        hub.add("unauthenticated", endpoint());
        List<RegistrationRequestContext> requests = new ArrayList<>();
        OutNode out = new OutNode().setSendOperationFactory(env -> null);
        out.register(MessageType.REGISTER_COMMANDS, new OutboundHandler<RegistrationRequestContext>() {
            @Override
            public SendOperation accept(RegistrationRequestContext request) {
                requests.add(request);
                if (callbackThenThrow && requests.size() == 1) {
                    request.resultCallback().accept(false);
                    throw new IllegalStateException("send failed after callback");
                }
                return null;
            }
        });
        ProxyServer proxy = (ProxyServer) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { ProxyServer.class }, (p, method, args) -> null);
        RegistrationManager registrations = new RegistrationManager(proxy, hub, VelocityConfig.defaults(), out,
                new CustomArgumentRegistry());
        Script previous = new Script(4, "removed", true, null, List.of(), null,
                List.of(new IdMapping("lobby", Location.BACKEND)), null, List.of(), List.of());
        registrations.load(List.of(previous));
        assertFalse(registrations.getScriptsForSession(hub.get("lobby").orElseThrow()).isEmpty());
        var config = new ConfigManager<>(dir, VelocityConfig.class);
        var scripts = new ScriptManager(dir, null);

        new ReloadCommand(config, scripts, registrations, hub, out).execute(sender);

        assertEquals(Set.of("lobby", "empty"), requests.stream().map(r -> r.session().id()).collect(Collectors.toSet()));
        assertEquals(2, requests.size());
        assertTrue(requests.stream().allMatch(r -> r.scripts().isEmpty()));
        assertTrue(messages.isEmpty());
        requests.getFirst().resultCallback().accept(true);
        requests.getFirst().resultCallback().accept(true);
        assertTrue(messages.isEmpty());
        requests.getLast().resultCallback().accept(true);
        assertFalse(messages.isEmpty());
        int lines = messages.size();
        requests.getLast().resultCallback().accept(false);
        assertEquals(lines, messages.size());
    }

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
}
