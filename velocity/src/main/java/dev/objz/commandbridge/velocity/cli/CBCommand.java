package dev.objz.commandbridge.velocity.cli;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.executors.CommandExecutor;
import dev.jorel.commandapi.executors.ResultingCommandExecutor;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ProxyServer;

import dev.objz.commandbridge.config.ConfigKeys;
import dev.objz.commandbridge.config.ConfigManager;
import dev.objz.commandbridge.config.model.VelocityConfig;
import dev.objz.commandbridge.net.OutNode;
import dev.objz.commandbridge.scripting.model.Script;
import dev.objz.commandbridge.velocity.RegistrationManager;
import dev.objz.commandbridge.velocity.ScriptManager;
import dev.objz.commandbridge.velocity.cli.subcommands.ClientCommand;
import dev.objz.commandbridge.velocity.cli.subcommands.ConfigCommand;
import dev.objz.commandbridge.velocity.cli.subcommands.DebugCommand;
import dev.objz.commandbridge.velocity.cli.subcommands.DumpCommand;
import dev.objz.commandbridge.velocity.cli.subcommands.HelpCommand;
import dev.objz.commandbridge.velocity.cli.subcommands.InfoCommand;
import dev.objz.commandbridge.velocity.cli.subcommands.MigrateCommand;
import dev.objz.commandbridge.velocity.cli.subcommands.ReloadCommand;
import dev.objz.commandbridge.velocity.cli.subcommands.ScriptCommand;
import dev.objz.commandbridge.velocity.cli.subcommands.TaskCommand;
import dev.objz.commandbridge.velocity.dispatch.CommandEntry;
import dev.objz.commandbridge.velocity.dispatch.model.ScheduledTask;
import dev.objz.commandbridge.velocity.net.session.ClientSession;
import dev.objz.commandbridge.velocity.net.session.SessionHub;
import dev.objz.commandbridge.velocity.util.PlayerTracker;
import dev.objz.commandbridge.velocity.util.UserCache;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class CBCommand {

    private final ProxyServer proxy;
    private final ConfigManager<VelocityConfig> configManager;
    private final ScriptManager scriptManager;
    private final RegistrationManager registrationManager;
    private final SessionHub sessionHub;
    private final OutNode outNode;
    private final VelocityConfig config;
    private final Path dataDir;
    private final CommandEntry commandEntry;
    private final UserCache userCache;
    private final PlayerTracker playerTracker;

    public CBCommand(
            ProxyServer proxy,
            ConfigManager<VelocityConfig> configManager,
            ScriptManager scriptManager,
            RegistrationManager registrationManager,
            SessionHub sessionHub,
            OutNode outNode,
            VelocityConfig config,
            Path dataDir,
            CommandEntry commandEntry,
            UserCache userCache,
            PlayerTracker playerTracker) {

        this.proxy = proxy;
        this.configManager = configManager;
        this.scriptManager = scriptManager;
        this.registrationManager = registrationManager;
        this.sessionHub = sessionHub;
        this.outNode = outNode;
        this.config = config;
        this.dataDir = dataDir;
        this.commandEntry = commandEntry;
        this.userCache = userCache;
        this.playerTracker = playerTracker;
    }

    public void register() {
        var help = new HelpCommand();
        var info = new InfoCommand();
        var reload = new ReloadCommand(configManager, scriptManager, registrationManager,
                sessionHub, outNode);
        var debug = new DebugCommand();
        var dump = new DumpCommand(registrationManager, sessionHub, outNode, scriptManager,
                config, dataDir);
        var migrate = new MigrateCommand(scriptManager.scriptsDir());
        var script = new ScriptCommand(scriptManager, registrationManager);
        var task = new TaskCommand(commandEntry.scheduler(), userCache);
        var client = new ClientCommand(sessionHub, outNode, playerTracker, userCache, config);
        var configCmd = new ConfigCommand(configManager);

        ArgumentSuggestions<CommandSource> clientIds = ArgumentSuggestions
                .<CommandSource>strings(info0 -> namesOf(sessionHub));

        ArgumentSuggestions<CommandSource> scriptNames = ArgumentSuggestions
                .<CommandSource>strings(info0 -> namesOfScripts(scriptManager.loaded()));

        ArgumentSuggestions<CommandSource> enabledScriptNames = ArgumentSuggestions
                .<CommandSource>strings(info0 -> namesOfScripts(scriptManager.enabled()));

        ArgumentSuggestions<CommandSource> disabledScriptNames = ArgumentSuggestions
                .<CommandSource>strings(info0 -> namesOfScripts(scriptManager.disabled()));

        ArgumentSuggestions<CommandSource> scriptGroups = ArgumentSuggestions
                .<CommandSource>strings(info0 -> ScriptCommand.GROUPS.toArray(String[]::new));

        ArgumentSuggestions<CommandSource> taskIds = ArgumentSuggestions
                .<CommandSource>strings(info0 -> taskIdsOf(commandEntry.scheduler().tasks()));

        ArgumentSuggestions<CommandSource> configKeys = ArgumentSuggestions
                .<CommandSource>strings(info0 -> ConfigKeys.topLevelKeysOf(VelocityConfig.class)
                        .toArray(String[]::new));

        new CommandAPICommand("commandbridge")
                .withAliases("cb")
                .withPermission("commandbridge.admin")

                // /cb -> help
                .executes((ResultingCommandExecutor) (sender, args) -> {
                    help.execute(sender);
                    return 1;
                })

                .withSubcommand(new CommandAPICommand("help")
                        .executes((CommandExecutor) (sender, args) -> help.execute(sender)))

                .withSubcommand(new CommandAPICommand("info")
                        .executes((CommandExecutor) (sender, args) -> info.execute(sender)))

                .withSubcommand(new CommandAPICommand("reload")
                        .executes((CommandExecutor) (sender, args) -> reload.execute(sender)))

                .withSubcommand(new CommandAPICommand("debug")
                        .executes((CommandExecutor) (sender, args) -> debug.execute(sender)))

                .withSubcommand(new CommandAPICommand("dump")
                        .executes((CommandExecutor) (sender, args) -> dump.execute(sender)))

                .withSubcommand(new CommandAPICommand("migrate")
                        .executes((CommandExecutor) (sender, args) -> migrate.execute(sender)))

                // /cb script ...
                .withSubcommand(new CommandAPICommand("script")
                        .withSubcommand(new CommandAPICommand("list")
                                .withOptionalArguments(new IntegerArgument("page"))
                                .executes((CommandExecutor) (sender, args) -> {
                                    Integer page = (Integer) args.get("page");
                                    script.list(sender, page != null ? page : 1);
                                }))
                        .withSubcommand(new CommandAPICommand("show")
                                .withArguments(new StringArgument("name")
                                        .replaceSuggestions(scriptNames))
                                .withOptionalArguments(new StringArgument("group")
                                        .replaceSuggestions(scriptGroups))
                                .executes((CommandExecutor) (sender, args) -> script.show(
                                        sender, (String) args.get("name"),
                                        (String) args.get("group"))))
                        .withSubcommand(new CommandAPICommand("enable")
                                .withArguments(new StringArgument("name")
                                        .replaceSuggestions(disabledScriptNames))
                                .executes((CommandExecutor) (sender, args) -> script.enable(
                                        sender, (String) args.get("name"))))
                        .withSubcommand(new CommandAPICommand("disable")
                                .withArguments(new StringArgument("name")
                                        .replaceSuggestions(enabledScriptNames))
                                .executes((CommandExecutor) (sender, args) -> script.disable(
                                        sender, (String) args.get("name")))))

                // /cb task ...
                .withSubcommand(new CommandAPICommand("task")
                        .withSubcommand(new CommandAPICommand("list")
                                .withOptionalArguments(new IntegerArgument("page"))
                                .executes((CommandExecutor) (sender, args) -> {
                                    Integer page = (Integer) args.get("page");
                                    task.list(sender, page != null ? page : 1);
                                }))
                        .withSubcommand(new CommandAPICommand("clear")
                                .withArguments(new StringArgument("id")
                                        .replaceSuggestions(taskIds))
                                .executes((CommandExecutor) (sender, args) -> task.clear(
                                        sender, (String) args.get("id")))))

                // /cb client ...
                .withSubcommand(new CommandAPICommand("client")
                        .withSubcommand(new CommandAPICommand("list")
                                .executes((CommandExecutor) (sender, args) -> client.list(sender)))
                        .withSubcommand(new CommandAPICommand("ping")
                                .withOptionalArguments(new StringArgument("id")
                                        .replaceSuggestions(clientIds))
                                .executes((CommandExecutor) (sender, args) -> client.ping(
                                        sender, (String) args.get("id"))))
                        .withSubcommand(new CommandAPICommand("players")
                                .withArguments(new StringArgument("id")
                                        .replaceSuggestions(clientIds))
                                .executes((CommandExecutor) (sender, args) -> client.players(
                                        sender, (String) args.get("id")))))

                // /cb config ...
                .withSubcommand(new CommandAPICommand("config")
                        .withSubcommand(new CommandAPICommand("show")
                                .withOptionalArguments(new StringArgument("key")
                                        .replaceSuggestions(configKeys))
                                .executes((CommandExecutor) (sender, args) -> configCmd.show(
                                        sender, (String) args.get("key"))))
                        .withSubcommand(new CommandAPICommand("reload")
                                .executes((CommandExecutor) (sender, args) -> configCmd.reload(
                                        sender))))

                .register();
    }

    private static String[] namesOf(SessionHub hub) {
        List<String> ids = new ArrayList<>();
        for (ClientSession s : hub) {
            if (s.id() != null) {
                ids.add(s.id());
            }
        }
        return ids.toArray(String[]::new);
    }

    private static String[] namesOfScripts(List<Script> scripts) {
        List<String> names = new ArrayList<>();
        for (Script s : scripts) {
            if (s != null && s.name() != null) {
                names.add(s.name());
            }
        }
        return names.toArray(String[]::new);
    }

    private static String[] taskIdsOf(Iterable<ScheduledTask> tasks) {
        List<String> ids = new ArrayList<>();
        for (ScheduledTask t : tasks) {
            if (t.id() != null) {
                ids.add(t.id().toString());
            }
        }
        return ids.toArray(String[]::new);
    }
}
