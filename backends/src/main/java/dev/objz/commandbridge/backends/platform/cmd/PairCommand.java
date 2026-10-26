package dev.objz.commandbridge.backends.platform.cmd;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.executors.CommandExecutor;
import dev.objz.commandbridge.backends.platform.PlatformAdapter;
import dev.objz.commandbridge.backends.platform.PlatformDetector;
import dev.objz.commandbridge.backends.setup.PairClient;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.util.MM;
import net.kyori.adventure.audience.Audience;

import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

/** /cbc pair &lt;code&gt;: takes this server's config from the proxy's setup. */
public final class PairCommand {

    private PairCommand() {
    }

    // one pairing waits for its confirm at a time
    private static final AtomicReference<PairClient.Pending> WAITING = new AtomicReference<>();

    public static CommandAPICommand subcommand(PlatformAdapter<?> adapter, Path dataDir, String version) {
        return new CommandAPICommand("pair")
                .withArguments(new GreedyStringArgument("code"))
                .executes((CommandExecutor) (sender, args) -> {
                    // the code is half the secret: keep it out of chat and out of the server log
                    if (sender instanceof Player player) {
                        ((Audience) player).sendMessage(MM.error("Run /cbc pair in the server console"));
                        return;
                    }
                    String input = ((String) args.get("code")).trim();
                    if (input.equalsIgnoreCase("confirm")) {
                        confirm(sender, adapter);
                    } else if (input.equalsIgnoreCase("cancel")) {
                        PairClient.Pending pending = WAITING.getAndSet(null);
                        if (pending == null) {
                            say(sender, Log::warn, MM::warn, "No pairing is waiting");
                        } else {
                            pending.cancel();
                            say(sender, Log::info, MM::muted, "Pairing cancelled, config.yml is unchanged");
                        }
                    } else {
                        fetch(sender, dataDir, version, input);
                    }
                });
    }

    private static void fetch(CommandSender sender, Path dataDir, String version, String code) {
        say(sender, Log::info, MM::muted, "Pairing with the proxy...");
        PairClient.overRelay(dataDir)
                .pair(code, PlatformDetector.detectPlatform().name(), version)
                .whenComplete((pending, error) -> {
                    if (error != null) {
                        Throwable t = error.getCause() != null ? error.getCause() : error;
                        say(sender, Log::error, MM::error, t.getMessage());
                        return;
                    }
                    PairClient.Pending before = WAITING.getAndSet(pending);
                    if (before != null)
                        before.cancel();
                    say(sender, Log::warn, MM::warn, "This server would join proxy " + pending.proxyId() + " at " + pending.where()
                            + " as " + pending.config().clientId());
                    say(sender, Log::warn, MM::warn, "Run /cbc pair confirm to save it, or /cbc pair cancel");
                    CompletableFuture.delayedExecutor(5, TimeUnit.MINUTES).execute(() -> {
                        if (WAITING.compareAndSet(pending, null)) {
                            pending.cancel();
                            say(sender, Log::info, MM::muted, "The pairing was not confirmed and has been cancelled");
                        }
                    });
                });
    }

    private static void confirm(CommandSender sender, PlatformAdapter<?> adapter) {
        PairClient.Pending pending = WAITING.getAndSet(null);
        if (pending == null) {
            say(sender, Log::warn, MM::warn, "No pairing is waiting. Run /cbc pair <code> first");
            return;
        }
        pending.confirm().whenComplete((config, error) -> {
            if (error != null) {
                say(sender, Log::error, MM::error, error.getMessage());
                return;
            }
            say(sender, Log::success, MM::ok, "Paired with " + pending.proxyId());
            adapter.applyNewConfig();
        });
    }

    // the console gets log lines in the logger's colors, a player gets a chat message
    private static void say(CommandSender sender, Consumer<String> log, Function<String, Component> chat, String text) {
        if (sender instanceof Player player) {
            ((Audience) player).sendMessage(chat.apply(text));
        } else {
            log.accept(text);
        }
    }
}
