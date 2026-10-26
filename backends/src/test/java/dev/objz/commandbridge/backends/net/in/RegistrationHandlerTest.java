package dev.objz.commandbridge.backends.net.in;

import com.fasterxml.jackson.databind.JsonNode;
import dev.objz.commandbridge.backends.TestFixtures;
import dev.objz.commandbridge.backends.net.client.BackendClient;
import dev.objz.commandbridge.backends.net.connection.ClientStatus;
import dev.objz.commandbridge.cmd.CommandRegistryInterface;
import dev.objz.commandbridge.net.InNode;
import dev.objz.commandbridge.net.OutNode;
import dev.objz.commandbridge.net.SendOperation;
import dev.objz.commandbridge.net.payloads.cmd.CommandStub;
import dev.objz.commandbridge.net.payloads.cmd.RegisterCommands;
import dev.objz.commandbridge.net.payloads.feedback.Feedback;
import dev.objz.commandbridge.net.proto.Envelope;
import dev.objz.commandbridge.net.proto.MessageType;
import dev.objz.commandbridge.scripting.model.enums.Location;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RegistrationHandlerTest {

    private final List<String> registryCalls = new ArrayList<>();
    private boolean failUnregister;

    private final CommandRegistryInterface registry = new CommandRegistryInterface() {
        @Override
        public void register(CommandStub stub) {
            registryCalls.add("register:" + stub.name());
            if (stub.name().startsWith("bad")) {
                throw new IllegalArgumentException("unsupported argument");
            }
        }

        @Override
        public void unregisterAll() {
            registryCalls.add("unregisterAll");
            if (failUnregister) {
                throw new IllegalStateException("commandapi not ready");
            }
        }
    };

    private final FakeClient client = new FakeClient();
    private final RegistrationHandler handler = new RegistrationHandler(client, registry);
    private final RecordingEndpoint endpoint = new RecordingEndpoint().wire(handler);

    @BeforeAll
    static void setUpLog() {
        TestFixtures.ensureLog();
    }

    private void receive(JsonNode payload) {
        handler.accept(endpoint, new Envelope(1, UUID.randomUUID(), MessageType.REGISTER_COMMANDS,
                "proxy-1", "survival-1", 0L, payload));
    }

    private static JsonNode commands(String... names) {
        List<CommandStub> stubs = new ArrayList<>();
        for (String name : names) {
            stubs.add(new CommandStub(name, List.of(), null, List.of()));
        }
        return Envelope.MAPPER.valueToTree(new RegisterCommands(stubs));
    }

    private Feedback feedback() throws Exception {
        return endpoint.onlyReply(MessageType.REGISTER_COMMANDS_RESULT, Feedback.class);
    }

    @Test
    void validRequestReplacesRegisteredCommands() throws Exception {
        receive(commands("heal", "feed"));

        assertEquals(List.of("unregisterAll", "register:heal", "register:feed"), registryCalls);
        Feedback feedback = feedback();
        assertEquals(2, feedback.requested());
        assertEquals(2, feedback.succeeded());
        assertEquals(List.of("heal", "feed"),
                handler.snapshotRegisteredCommands().stream().map(CommandStub::name).toList());
    }

    @Test
    void failingCommandIsReportedAndOthersStillRegister() throws Exception {
        receive(commands("bad-one", "heal"));

        Feedback feedback = feedback();
        assertEquals(2, feedback.requested());
        assertEquals(1, feedback.succeeded());
        assertEquals(1, feedback.failed());
        assertEquals(List.of("register 'bad-one': unsupported argument"), feedback.errors());
    }

    @Test
    void malformedRequestUnregistersEverything() throws Exception {
        receive(commands("heal"));
        endpoint.sent.clear();
        registryCalls.clear();

        receive(Envelope.MAPPER.valueToTree("not a registration"));

        assertEquals(List.of("unregisterAll"), registryCalls);
        assertTrue(handler.snapshotRegisteredCommands().isEmpty());
        assertEquals(List.of("Received empty registration request"), feedback().warnings());
    }

    @Test
    void emptyRequestUnregistersEverything() throws Exception {
        receive(commands());

        assertEquals(List.of("unregisterAll"), registryCalls);
        assertEquals(0, feedback().requested());
    }

    @Test
    void unregisterFailureDoesNotBlockRegistration() throws Exception {
        failUnregister = true;

        receive(commands("heal"));

        assertEquals(List.of("unregisterAll", "register:heal"), registryCalls);
        assertEquals(1, feedback().succeeded());
    }

    @Test
    void senderBecomesServerIdWhenUnset() {
        receive(commands("heal"));

        assertEquals("proxy-1", client.serverId);
    }

    @Test
    void existingServerIdIsKept() {
        client.serverId = "proxy-main";

        receive(commands("heal"));

        assertEquals("proxy-main", client.serverId);
    }

    private static final class FakeClient implements BackendClient {
        String serverId;

        @Override
        public String serverId() {
            return serverId;
        }

        @Override
        public void setServerId(String serverId) {
            this.serverId = serverId;
        }

        @Override
        public void start() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void reconnect() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void scheduleReconnection() {
            throw new UnsupportedOperationException();
        }

        @Override
        public SendOperation send(Envelope request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ClientStatus status() {
            return ClientStatus.AUTH_OK;
        }

        @Override
        public InNode inboundRouter() {
            throw new UnsupportedOperationException();
        }

        @Override
        public OutNode outboundRouter() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setLocation(Location location) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void onAuthenticated(Runnable callback) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void close() {
        }
    }
}
