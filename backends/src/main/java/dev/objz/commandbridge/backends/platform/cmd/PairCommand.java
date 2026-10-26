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

import java.nio.file.Files;
import java.nio.file.Path;

/** /cbc pair &lt;code&gt;: takes this server's config from the proxy's setup. */
public final class PairCommand {

    private PairCommand() {
    }

    public static CommandAPICommand subcommand(PlatformAdapter<?> adapter, Path dataDir, String version) {
        return new CommandAPICommand("pair")
                .withArguments(new GreedyStringArgument("code"))
                .executes((CommandExecutor) (sender, args) -> {
                    Audience src = (Audience) sender;
                    boolean hadConfig = Files.exists(dataDir.resolve("config.yml"));
                    MM.msg().space().line(MM.muted("Pairing with the proxy...")).send(src);
                    PairClient.overRelay(dataDir)
                            .pair((String) args.get("code"), PlatformDetector.detectPlatform().name(), version)
                            .whenComplete((paired, error) -> {
                                if (error != null) {
                                    Throwable t = error.getCause() != null ? error.getCause() : error;
                                    Log.warn("Pairing failed: {}", t.getMessage());
                                    MM.msg().space().line(MM.error(t.getMessage())).send(src);
                                    return;
                                }
                                String saved = hadConfig
                                        ? "Saved config.yml (old one kept as config.yml.bak). Connecting..."
                                        : "Saved config.yml. Connecting...";
                                MM.msg().space().line(MM.ok("Paired with " + paired.proxyId() + ". " + saved)).send(src);
                                adapter.applyNewConfig();
                            });
                });
    }
}
