package dev.objz.commandbridge.velocity.cli;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.executors.CommandExecutor;
import dev.jorel.commandapi.executors.ResultingCommandExecutor;

import com.velocitypowered.api.command.CommandSource;

import dev.objz.commandbridge.config.ConfigManager;
import dev.objz.commandbridge.config.model.VelocityConfig;
import dev.objz.commandbridge.net.OutNode;
import dev.objz.commandbridge.velocity.RegistrationManager;
import dev.objz.commandbridge.velocity.ScriptManager;
import dev.objz.commandbridge.velocity.cli.subcommands.*;
import dev.objz.commandbridge.velocity.net.session.ClientSession;
import dev.objz.commandbridge.velocity.net.session.SessionHub;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class CBCommand {

    private final ConfigManager configManager;
    private final ScriptManager scriptManager;
    private final RegistrationManager registrationManager;
    private final SessionHub sessionHub;
    private final OutNode<Object> outNode;
    private final VelocityConfig config;
    private final Path dataDir;

    public CBCommand(
            ConfigManager configManager,
            ScriptManager scriptManager,
            RegistrationManager registrationManager,
            SessionHub sessionHub,
            OutNode<Object> outNode,
            VelocityConfig config,
            Path dataDir) {

        this.configManager = configManager;
        this.scriptManager = scriptManager;
        this.registrationManager = registrationManager;
        this.sessionHub = sessionHub;
        this.outNode = outNode;
        this.config = config;
        this.dataDir = dataDir;
    }

    public void register() {
        ArgumentSuggestions<CommandSource> clientIdSuggestions = ArgumentSuggestions
                .<CommandSource>strings(info -> {
                    List<String> ids = new ArrayList<>();
                    for (ClientSession s : sessionHub) {
                        if (s.id() != null) {
                            ids.add(s.id());
                        }
                    }
                    return ids.toArray(String[]::new);
                });

        var help = new HelpCommand();
        var scripts = new ScriptsCommand(scriptManager);
        var reload = new ReloadCommand(configManager, scriptManager, registrationManager, sessionHub, outNode);
        var list = new ListCommand(sessionHub);
        var ping = new PingCommand(sessionHub, outNode, config);
        var debug = new DebugCommand();
        var dump = new DumpCommand(registrationManager, sessionHub, outNode, scriptManager, config, dataDir);
        var migrate = new MigrateCommand(scriptManager.scriptsDir(), dataDir);
        var infoCmd = new InfoCommand();

        new CommandAPICommand("commandbridge")
                .withAliases("cb")
                .withPermission("commandbridge.admin")

                // /cb -> help
                .executes((ResultingCommandExecutor) (sender, args) -> {
                    help.execute(sender);
                    return 1;
                })

                // /cb help
                .withSubcommand(new CommandAPICommand("help")
                        .executes((CommandExecutor) (sender, args) -> {
                            help.execute(sender);
                        }))

                // /cb info
                .withSubcommand(new CommandAPICommand("info")
                        .executes((CommandExecutor) (sender, args) -> {
                            infoCmd.execute(sender);
                        }))

                // /cb scripts [page]
                .withSubcommand(new CommandAPICommand("scripts")
                        .withOptionalArguments(new IntegerArgument("page"))
                        .executes((CommandExecutor) (sender, args) -> {
                            int page = 1;
                            if (args.get("page") != null) {
                                page = (int) args.get("page");
                            }
                            scripts.execute(sender, page);
                        }))

                // /cb reload
                .withSubcommand(new CommandAPICommand("reload")
                        .executes((CommandExecutor) (sender, args) -> {
                            reload.execute(sender);
                        }))

                // /cb list
                .withSubcommand(new CommandAPICommand("list")
                        .executes((CommandExecutor) (sender, args) -> {
                            list.execute(sender);
                        }))

                // /cb ping [clientId]
                .withSubcommand(new CommandAPICommand("ping")
                        .withOptionalArguments(new StringArgument("clientId")
                                .replaceSuggestions(clientIdSuggestions))
                        .executes((CommandExecutor) (sender, args) -> {
                            String id = (String) args.get("clientId");
                            ping.execute(sender, id);
                        }))

                // /cb debug
                .withSubcommand(new CommandAPICommand("debug")
                        .executes((CommandExecutor) (sender, args) -> {
                            debug.execute(sender);
                        }))

                // /cb dump
                .withSubcommand(new CommandAPICommand("dump")
                        .executes((CommandExecutor) (sender, args) -> {
                            dump.execute(sender);
                        }))

                // /cb migrate
                .withSubcommand(new CommandAPICommand("migrate")
                        .executes((CommandExecutor) (sender, args) -> {
                            migrate.execute(sender);
                        }))

                .register();
    }
}
