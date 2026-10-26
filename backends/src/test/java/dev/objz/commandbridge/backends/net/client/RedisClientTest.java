package dev.objz.commandbridge.backends.net.client;

import dev.objz.commandbridge.backends.TestFixtures;
import dev.objz.commandbridge.api.CommandBridgeProvider;
import dev.objz.commandbridge.backends.api.BackendCommandBridgeImpl;
import dev.objz.commandbridge.backends.platform.PlatformAdapter;
import dev.objz.commandbridge.backends.platform.ScheduleHandle;
import dev.objz.commandbridge.backends.platform.cmd.CommandExecutor;
import dev.objz.commandbridge.config.model.BackendsConfig;
import dev.objz.commandbridge.net.proto.Envelope;
import dev.objz.commandbridge.net.proto.MessageType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests that local message validation does not trigger a Redis reconnect. */
final class RedisClientTest {
    @BeforeAll
    static void setup() {
        TestFixtures.ensureLog();
    }

    @Test
    void oversizedSendFailsWithoutDisconnectingOrReconnecting() throws Exception {
        RecordingAdapter adapter = new RecordingAdapter();
        RedisClient client = new RedisClient(BackendsConfig.defaults(), Path.of("."), adapter);
        Field running = RedisClient.class.getDeclaredField("running");
        running.setAccessible(true);
        running.setBoolean(client, true);
        Method publish = RedisClient.class.getDeclaredMethod("publishToProxy", Envelope.class);
        publish.setAccessible(true);
        Envelope message = Envelope.make(MessageType.PLUGIN_MESSAGE, "lobby", "proxy",
                Envelope.MAPPER.getNodeFactory().textNode("x".repeat(1_048_577)));

        CompletableFuture<?> result = (CompletableFuture<?>) publish.invoke(client, message);

        CompletionException failure = assertThrows(CompletionException.class, result::join);
        assertInstanceOf(IllegalArgumentException.class, failure.getCause());
        assertTrue(running.getBoolean(client));
        assertEquals(0, adapter.schedules);
    }

    @Test
    void closeUnregistersApiAndStopsStatePolling() throws Exception {
        RedisClient client = new RedisClient(BackendsConfig.defaults(), Path.of("."), new RecordingAdapter());
        Field apiField = RedisClient.class.getDeclaredField("api");
        apiField.setAccessible(true);
        BackendCommandBridgeImpl api = (BackendCommandBridgeImpl) apiField.get(client);
        try {
            api.bootstrap();
            api.onConnectionStateChanged(state -> { });
            Field pollerField = BackendCommandBridgeImpl.class.getDeclaredField("statePoller");
            pollerField.setAccessible(true);
            ScheduledExecutorService poller = (ScheduledExecutorService) pollerField.get(api);
            assertEquals(api, CommandBridgeProvider.get());

            client.close();
            client.close();

            assertTrue(poller.isShutdown());
            assertThrows(IllegalStateException.class, CommandBridgeProvider::get);
        } finally {
            api.shutdown();
            client.close();
        }
    }

    private static final class RecordingAdapter implements PlatformAdapter<Object> {
        private int schedules;

        @Override
        public void start(PlatformEnv env) {
        }

        @Override
        public void stop() {
        }

        @Override
        public CommandExecutor getCommandExecutor() {
            return null;
        }

        @Override
        public Set<UUID> getOnlinePlayerIds() {
            return Set.of();
        }

        @Override
        public ScheduleHandle runSchedule(Runnable task, Duration timeout, Duration interval) {
            schedules++;
            return () -> {
            };
        }
    }
}
