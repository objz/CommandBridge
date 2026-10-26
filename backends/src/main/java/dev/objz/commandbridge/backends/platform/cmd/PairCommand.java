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
import java.util.function.Consumer;
import java.util.function.Function;

/** /cbc pair &lt;code&gt;: takes this server's config from the proxy's setup. */
public final class PairCommand {

    private PairCommand() {
    }

    public static CommandAPICommand subcommand(PlatformAdapter<?> adapter, Path dataDir, String version) {
        return new CommandAPICommand("pair")
                .withArguments(new GreedyStringArgument("code"))
                .executes((CommandExecutor) (sender, args) -> {
                    say(sender, Log::info, MM::muted, "Pairing with the proxy...");
                    PairClient.overRelay(dataDir)
                            .pair((String) args.get("code"), PlatformDetector.detectPlatform().name(), version)
                            .whenComplete((paired, error) -> {
                                if (error != null) {
                                    Throwable t = error.getCause() != null ? error.getCause() : error;
                                    say(sender, Log::error, MM::error, t.getMessage());
                                    return;
                                }
                                say(sender, Log::success, MM::ok, "Paired with " + paired.proxyId());
                                adapter.applyNewConfig();
                            });
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
