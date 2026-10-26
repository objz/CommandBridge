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

public final class SetupCommand extends AbstractCliCommand {

    private final ProxyServer proxy;
    private final ConfigManager<VelocityConfig> configManager;
    private final SessionHub sessionHub;
    private final Path dataDir;
    private volatile SetupSession active;

    public SetupCommand(ProxyServer proxy, ConfigManager<VelocityConfig> configManager, SessionHub sessionHub,
            Path dataDir) {
        this.proxy = proxy;
        this.configManager = configManager;
        this.sessionHub = sessionHub;
        this.dataDir = dataDir;
    }

    public void open(CommandSource sender) {
        SetupSession current = active;
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
        // Velocity reads velocity.toml from its working directory
        SetupSession.open(proxy, configManager, sessionHub, dataDir, Path.of("velocity.toml"), () -> {
        }).whenComplete((session, error) -> {
            if (error != null) {
                Report.of("Setup").error("Could not open the setup: " + rootMessage(error)).send(sender);
                return;
            }
            active = session;
            Report.of("Setup")
                    .success("Setup opened")
                    .link("Setup", session.url(), "Click to open")
                    .muted("Anyone with this link can plan changes. Nothing is applied until /cb setup confirm")
                    .send(sender);
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
            Report report = result.errors().isEmpty()
                    ? Report.of("Setup").success("Applied the setup")
                    : Report.of("Setup").error("Not everything was applied");
            result.errors().forEach(report::bullet);
            if (result.restartNeeded())
                report.warn("Restart the proxy to switch to the new connection settings.");
            report.send(sender);
        } catch (Exception e) {
            Report.of("Setup").error("Could not apply the setup: " + rootMessage(e)).send(sender);
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
