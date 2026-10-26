package dev.objz.commandbridge.backends.net.out;

import com.fasterxml.jackson.databind.JsonNode;
import dev.jorel.commandapi.executors.CommandArguments;
import dev.objz.commandbridge.backends.TestFixtures;
import dev.objz.commandbridge.backends.net.out.ctx.InvokedCommandContext;
import dev.objz.commandbridge.net.Endpoint;
import dev.objz.commandbridge.net.OutNode;
import dev.objz.commandbridge.net.ResponseAwaiter;
import dev.objz.commandbridge.net.SendOperation;
import dev.objz.commandbridge.net.payloads.cmd.CommandStub;
import dev.objz.commandbridge.net.proto.Envelope;
import dev.objz.commandbridge.net.proto.MessageType;
import dev.objz.commandbridge.scripting.model.enums.ArgType;
import dev.objz.commandbridge.scripting.model.records.mapping.ArgMapping;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The event reads senders and argument values by reflection, so plain fake objects with the
 * same getter names stand in for Bukkit/Velocity types.
 */
final class InvokedCommandEventTest {

    private static final UUID STEVE = UUID.fromString("00000000-0000-0000-0000-0000000057e5");

    public static final class FakePlayer {
        private final UUID uuid;
        private final String name;

        public FakePlayer(UUID uuid, String name) {
            this.uuid = uuid;
            this.name = name;
        }

        public UUID getUniqueId() {
            return uuid;
        }

        public String getName() {
            return name;
        }

        public FakeType getType() {
            return FakeType.PLAYER;
        }
    }

    public enum FakeType {
        PLAYER
    }

    public static final class FakeConsoleCommandSender {
    }

    public static final class FakeWorld {
        public String getName() {
            return "world_nether";
        }
    }

    public static final class FakeLocation {
        public double getX() {
            return 1.5;
        }

        public double getY() {
            return 64;
        }

        public double getZ() {
            return -3;
        }

        public FakeWorld getWorld() {
            return new FakeWorld();
        }
    }

    private final List<Envelope> sent = new ArrayList<>();
    private OutNode outNode;

    @BeforeAll
    static void setUpLog() {
        TestFixtures.ensureLog();
    }

    @BeforeEach
    void setUp() {
        Endpoint endpoint = new Endpoint() {
            @Override
            public CompletableFuture<Void> send(Envelope env) {
                sent.add(env);
                return CompletableFuture.completedFuture(null);
            }

            @Override
            public boolean isOpen() {
                return true;
            }
        };
        outNode = new OutNode()
                .setSendOperationFactory(env -> new SendOperation(endpoint, env, new ResponseAwaiter()))
                .setClientId("survival-1")
                .setServerId("proxy-1");
        outNode.register(MessageType.INVOKED_COMMAND, new InvokedCommandEvent());
    }

    private JsonNode invoke(Object sender, Map<String, Object> values, ArgMapping... mappings) {
        sent.clear();
        var args = new CommandArguments(values.values().toArray(), new HashMap<>(values), new String[0],
                Map.of(), "/test");
        outNode.send(MessageType.INVOKED_COMMAND,
                new InvokedCommandContext("test", sender, args, new CommandStub("test", List.of(), null, List.of(mappings))));
        assertEquals(1, sent.size());
        Envelope env = sent.getFirst();
        assertEquals(MessageType.INVOKED_COMMAND, env.type());
        assertEquals("survival-1", env.from());
        assertEquals("proxy-1", env.to());
        return env.payload();
    }

    private static ArgMapping arg(String name, ArgType type) {
        return new ArgMapping(name, false, type, null);
    }

    private JsonNode singleValue(Object raw, ArgType type) {
        Map<String, Object> values = new HashMap<>();
        if (raw != null) {
            values.put("value", raw);
        }
        JsonNode arg = invoke(new FakeConsoleCommandSender(), values, arg("value", type)).get("args").get(0);
        assertEquals(type.name(), arg.get("type").asText());
        return arg.get("value");
    }

    @Test
    void playerSenderIsMappedByReflection() {
        JsonNode sender = invoke(new FakePlayer(STEVE, "Steve"), Map.of()).get("sender");

        assertEquals("PLAYER", sender.get("type").asText());
        assertEquals("Steve", sender.get("name").asText());
        assertEquals(STEVE.toString(), sender.get("uuid").asText());
    }

    @Test
    void consoleSenderIsRecognisedByClassName() {
        JsonNode sender = invoke(new FakeConsoleCommandSender(), Map.of()).get("sender");

        assertEquals("CONSOLE", sender.get("type").asText());
    }

    @Test
    void unknownSenderIsReportedAsOther() {
        JsonNode sender = invoke(new Object(), Map.of()).get("sender");

        assertEquals("OTHER", sender.get("type").asText());
        assertEquals("Object", sender.get("className").asText());
    }

    @Test
    void integerValueIsKept() {
        assertEquals(42, singleValue(42, ArgType.INTEGER).asInt());
        assertEquals(7, singleValue("7", ArgType.INTEGER).asInt());
    }

    @Test
    void unparsableIntegerBecomesZero() {
        assertEquals(0, singleValue("abc", ArgType.INTEGER).asInt());
    }

    @Test
    void omittedOptionalIntegerStaysEmpty() {
        JsonNode value = singleValue(null, ArgType.INTEGER);

        assertTrue(value.isNull(), "omitted optional INTEGER should not become " + value);
    }

    @Test
    void booleanIsParsed() {
        assertTrue(singleValue(true, ArgType.BOOLEAN).asBoolean());
        assertTrue(singleValue("TRUE", ArgType.BOOLEAN).asBoolean());
        assertEquals(false, singleValue("yes", ArgType.BOOLEAN).asBoolean());
    }

    @Test
    void locationIsMappedWithWorldName() {
        JsonNode location = singleValue(new FakeLocation(), ArgType.LOCATION);

        assertEquals("world_nether", location.get("world").asText());
        assertEquals(1.5, location.get("x").asDouble());
        assertEquals(64.0, location.get("y").asDouble());
        assertEquals(-3.0, location.get("z").asDouble());
    }

    @Test
    void playersBecomeEntityRefs() {
        JsonNode refs = singleValue(List.of(new FakePlayer(STEVE, "Steve"), "Alex"), ArgType.PLAYERS);

        assertEquals(2, refs.size());
        assertEquals("PLAYER", refs.get(0).get("type").asText());
        assertEquals(STEVE.toString(), refs.get(0).get("uuid").asText());
        assertEquals("Steve", refs.get(0).get("name").asText());
        assertEquals("Alex", refs.get(1).get("name").asText());
        assertTrue(refs.get(1).get("uuid").isNull());
    }

    @Test
    void nonCollectionPlayersBecomeEmptyList() {
        JsonNode refs = singleValue("Steve", ArgType.PLAYERS);

        assertTrue(refs.isArray());
        assertEquals(0, refs.size());
    }

    @Test
    void textValuesAreStringified() {
        assertEquals("hello world", singleValue("hello world", ArgType.GREEDY_STRING).asText());
    }

    @Test
    void argumentsKeepStubOrder() {
        JsonNode args = invoke(new FakeConsoleCommandSender(), Map.of("b", 2, "a", "x"),
                arg("a", ArgType.STRING), arg("b", ArgType.INTEGER)).get("args");

        assertEquals("x", args.get(0).get("value").asText());
        assertEquals(2, args.get(1).get("value").asInt());
    }
}
