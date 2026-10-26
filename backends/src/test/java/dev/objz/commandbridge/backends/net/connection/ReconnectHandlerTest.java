package dev.objz.commandbridge.backends.net.connection;

import dev.objz.commandbridge.backends.TestFixtures;
import dev.objz.commandbridge.backends.platform.PlatformAdapter;
import dev.objz.commandbridge.backends.platform.ScheduleHandle;
import dev.objz.commandbridge.backends.platform.cmd.CommandExecutor;
import dev.objz.commandbridge.config.model.BackendsConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReconnectHandlerTest {

    @BeforeAll
    static void setUp() {
        TestFixtures.ensureLog();
    }

    /** Records schedules and never runs them, like a platform task that ends itself once its time is up. */
    static final class SilentScheduler implements PlatformAdapter<Object> {
        final List<Runnable> scheduled = new ArrayList<>();

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
            scheduled.add(task);
            return () -> {
            };
        }
    }

    private static BackendsConfig withReconnectTimeout(int seconds) {
        BackendsConfig d = BackendsConfig.defaults();
        return new BackendsConfig(d.clientId(), d.endpointType(), d.endpoints(), d.security(),
                new BackendsConfig.Timeouts(d.timeouts().authTimeout(), seconds, 0), d.debug());
    }

    @Test
    void aSecondScheduleWhileOneRunsIsIgnored() {
        var scheduler = new SilentScheduler();
        var handler = new ReconnectHandler(withReconnectTimeout(60), scheduler, () -> {
        });

        handler.scheduleReconnect();
        handler.scheduleReconnect();

        assertEquals(1, scheduler.scheduled.size());
    }

    @Test
    void afterTheWindowRanOutWithoutAWordANewReconnectStarts() throws Exception {
        var scheduler = new SilentScheduler();
        var handler = new ReconnectHandler(withReconnectTimeout(0), scheduler, () -> {
        });

        handler.scheduleReconnect();
        Thread.sleep(20);
        handler.scheduleReconnect();

        assertEquals(2, scheduler.scheduled.size());
    }
}
