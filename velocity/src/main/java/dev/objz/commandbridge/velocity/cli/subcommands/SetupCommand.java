package dev.objz.commandbridge.velocity.cli.subcommands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.objz.commandbridge.config.ConfigManager;
import dev.objz.commandbridge.config.model.VelocityConfig;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.util.MM;
import dev.objz.commandbridge.velocity.net.session.SessionHub;
import dev.objz.commandbridge.velocity.setup.SetupSession;
import dev.objz.commandbridge.velocity.ui.RenderContext;
import dev.objz.commandbridge.velocity.ui.Report;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

public final class SetupCommand extends AbstractCliCommand {

    private final ProxyServer proxy;
    private final ConfigManager<VelocityConfig> configManager;
    private final SessionHub sessionHub;
    private final Path dataDir;
    private final Runnable restart;
    private final Function<CommandSource, CompletableFuture<String>> openEditor;
    private static final long CONFIRM_WITHIN_MS = 60_000;
    private volatile SetupSession active;
    private long openingGeneration;
    private volatile long warnedAt;

    public SetupCommand(ProxyServer proxy, ConfigManager<VelocityConfig> configManager, SessionHub sessionHub,
            Path dataDir, Runnable restart, Function<CommandSource, CompletableFuture<String>> openEditor) {
        this.proxy = proxy;
        this.configManager = configManager;
        this.sessionHub = sessionHub;
        this.dataDir = dataDir;
        this.restart = restart;
        this.openEditor = openEditor;
    }

    /** On a proxy that's set up already, the first /cb setup only warns; a second one within a minute opens it. */
    public void open(CommandSource sender) {
        long now = System.currentTimeMillis();
        if (SetupSession.isConfigured(configManager.current(), dataDir) && now - warnedAt > CONFIRM_WITHIN_MS) {
            warnedAt = now;
            Report.of("Setup")
                    .warn("CommandBridge is already set up")
                    .muted("Running the setup again can replace config.yml and pair your servers again")
                    .muted("Run /cb setup again within a minute to continue")
                    .send(sender);
            return;
        }
        warnedAt = 0;
        start(sender, "Setup opened", "Anyone with this link can plan changes. Nothing is applied until /cb setup confirm");
    }

    /** A fresh install: open the setup without being asked and print the link in the console. */
    public void openOnFirstStart() {
        start(proxy.getConsoleCommandSource(), "CommandBridge isn't set up yet. Open this link to set up your network:",
                "The link stops working after 15 minutes unopened, run /cb setup for a new one");
    }

    private void start(CommandSource sender, String message, String note) {
        start(sender, message, note, () -> SetupSession.open(proxy, configManager, sessionHub, dataDir,
                Path.of("velocity.toml"), () -> openEditor.apply(sender), () -> {
                }));
    }

    synchronized void start(CommandSource sender, String message, String note,
            Supplier<CompletableFuture<SetupSession>> opener) {
        long generation = ++openingGeneration;
        SetupSession current = active;
        active = null;
        if (current != null) {
            current.closeOffers();
            if (current.isOpen())
                current.close();
        }

        if (new RenderContext(sender).isPlayer()) {
            sender.sendMessage(MM.muted("Connecting to the setup relay..."));
        } else {
            Log.info("Connecting to the setup relay...");
        }
        opener.get().whenComplete((session, error) -> {
            synchronized (this) {
                if (generation != openingGeneration) {
                    if (session != null) {
                        session.closeOffers();
                        session.close();
                    }
                    return;
                }
                if (error != null) {
                    Report.of("Setup").error("Could not open the setup: " + rootMessage(error)).send(sender);
                    return;
                }
                active = session;
                Report.of("Setup")
                        .success(message)
                        .link("Setup", session.url(), "Click to open")
                        .muted(note)
                        .send(sender);
            }
        });
    }

    public void confirm(CommandSource sender, String code) {
        SetupSession current = active;
        if (current == null || !current.isOpen()) {
            Report.of("Setup").warn("No setup is open").send(sender);
            return;
        }
        try {
            SetupSession.Result result = current.confirm(code);
            if (result == null) {
                Report.of("Setup").warn("No setup is waiting for that code").send(sender);
                return;
            }
            if (result.errors().isEmpty()) {
                say(sender, true, "Applied the setup");
            } else {
                say(sender, false, "Not everything was applied");
                result.errors().forEach(e -> say(sender, false, e));
            }
            // new connection settings: CommandBridge starts again with them, the proxy keeps running
            if (result.restartNeeded())
                restart.run();
        } catch (Exception e) {
            Report.of("Setup").error("Could not apply the setup: " + rootMessage(e)).send(sender);
        }
    }

    private static void say(CommandSource sender, boolean ok, String text) {
        if (new RenderContext(sender).isPlayer()) {
            sender.sendMessage(ok ? MM.ok(text) : MM.error(text));
        } else if (ok) {
            Log.success(text);
        } else {
            Log.error(text);
        }
    }

    private static String rootMessage(Throwable error) {
        Throwable t = error;
        while (t.getCause() != null) {
            t = t.getCause();
        }
        return t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName();
    }
}
