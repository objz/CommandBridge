package dev.objz.commandbridge.velocity;

import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.Scheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Reflection-based fakes for the parts of the Velocity API the plugin touches.
 * Unhandled methods throw so a test notices when production code starts using something new.
 */
public final class FakeProxy {

    public record Execution(CommandSource source, String command) {
    }

    public static final class ScheduledCall {
        public final Runnable task;
        public long delayMillis;
        public boolean repeating;
        public boolean scheduled;

        ScheduledCall(Runnable task) {
            this.task = task;
        }
    }

    public final Map<UUID, Player> players = new HashMap<>();
    public final List<Execution> executions = new ArrayList<>();
    public final List<ScheduledCall> scheduled = new ArrayList<>();
    public final List<String> consoleMessages = new ArrayList<>();
    public final ConsoleCommandSource console = proxy(ConsoleCommandSource.class, (method, args) -> switch (method) {
        case "hasPermission" -> true;
        case "sendMessage" -> {
            consoleMessages.add(plain((Component) args[args.length - 1]));
            yield null;
        }
        default -> unsupported(method);
    });
    public final ProxyServer server = proxy(ProxyServer.class, (method, args) -> switch (method) {
        case "getPlayer" -> args[0] instanceof UUID uuid
                ? Optional.ofNullable(players.get(uuid))
                : players.values().stream()
                        .filter(p -> p.getUsername().equalsIgnoreCase((String) args[0]))
                        .findFirst();
        case "getConsoleCommandSource" -> console;
        case "getCommandManager" -> commandManager();
        case "getScheduler" -> scheduler();
        default -> unsupported(method);
    });

    private CommandManager commandManager() {
        return proxy(CommandManager.class, (method, args) -> {
            if (method.equals("executeAsync")) {
                executions.add(new Execution((CommandSource) args[0], (String) args[1]));
                return CompletableFuture.completedFuture(true);
            }
            return unsupported(method);
        });
    }

    private Scheduler scheduler() {
        return proxy(Scheduler.class, (method, args) -> {
            if (method.equals("buildTask") && args[1] instanceof Runnable task) {
                ScheduledCall call = new ScheduledCall(task);
                scheduled.add(call);
                return taskBuilder(call);
            }
            return unsupported(method);
        });
    }

    private Scheduler.TaskBuilder taskBuilder(ScheduledCall call) {
        Scheduler.TaskBuilder[] self = new Scheduler.TaskBuilder[1];
        self[0] = proxy(Scheduler.TaskBuilder.class, (method, args) -> switch (method) {
            case "delay" -> {
                call.delayMillis = ((TimeUnit) args[1]).toMillis((Long) args[0]);
                yield self[0];
            }
            case "repeat" -> {
                call.repeating = true;
                yield self[0];
            }
            case "schedule" -> {
                call.scheduled = true;
                yield null;
            }
            default -> unsupported(method);
        });
        return self[0];
    }

    public Player addPlayer(UUID uuid, String name) {
        Player player = player(uuid, name);
        players.put(uuid, player);
        return player;
    }

    /** A player whose received chat messages are collected in {@code messages}. */
    public static Player player(UUID uuid, String name, List<String> messages) {
        return proxy(Player.class, (method, args) -> switch (method) {
            case "getUniqueId" -> uuid;
            case "getUsername" -> name;
            case "hasPermission" -> false;
            case "sendMessage" -> {
                messages.add(plain((Component) args[args.length - 1]));
                yield null;
            }
            default -> unsupported(method);
        });
    }

    public static Player player(UUID uuid, String name) {
        return player(uuid, name, new ArrayList<>());
    }

    public static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    private static Object unsupported(String method) {
        throw new UnsupportedOperationException("FakeProxy does not implement " + method);
    }

    @FunctionalInterface
    private interface Handler {
        Object handle(String method, Object[] args) throws Throwable;
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, Handler handler) {
        return (T) Proxy.newProxyInstance(FakeProxy.class.getClassLoader(), new Class<?>[] { type },
                (self, method, args) -> switch (method.getName()) {
                    case "equals" -> self == args[0];
                    case "hashCode" -> System.identityHashCode(self);
                    case "toString" -> "Fake" + type.getSimpleName();
                    default -> handler.handle(method.getName(), args == null ? new Object[0] : args);
                });
    }

    /** A command source that keeps the plain text of every message it receives. */
    public static final class RecordingSource extends VelocityTestDoubles.StubCommandSource {
        public final List<String> messages = new ArrayList<>();

        public RecordingSource() {
            super(Map.of());
        }

        @Override
        public void sendMessage(Component message) {
            messages.add(plain(message));
        }
    }
}
