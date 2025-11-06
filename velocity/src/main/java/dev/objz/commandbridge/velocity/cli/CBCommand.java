package dev.objz.commandbridge.velocity.cli;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.executors.CommandExecutor;
import dev.jorel.commandapi.executors.ResultingCommandExecutor;

import com.velocitypowered.api.command.CommandSource;

import dev.objz.commandbridge.config.ConfigManager;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.velocity.RegistrationManager;
import dev.objz.commandbridge.velocity.ScriptManager;
import dev.objz.commandbridge.velocity.cli.subcommands.*;
import dev.objz.commandbridge.velocity.net.WsServer;
import dev.objz.commandbridge.velocity.net.session.ClientSession;
import dev.objz.commandbridge.velocity.net.session.SessionHub;

import java.util.ArrayList;
import java.util.List;

public final class CBCommand {

	private final ConfigManager configManager;
	private final ScriptManager scriptManager;
	private final RegistrationManager registrationManager;
	private final SessionHub sessionHub;
	private final WsServer wsServer;

	public CBCommand(
			ConfigManager configManager,
			ScriptManager scriptManager,
			RegistrationManager registrationManager,
			SessionHub sessionHub,
			WsServer wsServer) {

		this.configManager = configManager;
		this.scriptManager = scriptManager;
		this.registrationManager = registrationManager;
		this.sessionHub = sessionHub;
		this.wsServer = wsServer;
	}

	public void register() {
		// Bind generic to CommandSource so replaceSuggestions(...) matches
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

		// Subcommand instances
		var help = new HelpCommand();
		var scriptsCmd = new ScriptsCommand(scriptManager);
		var reload = new ReloadCommand(configManager, scriptManager, registrationManager);
		var list = new ListCommand(sessionHub);
		var client = new InspectCommand(sessionHub);
		var ping = new PingCommand(sessionHub, wsServer);
		var debug = new DebugCommand();
		var dump = new DumpCommand(registrationManager, sessionHub);

		// Root command
		new CommandAPICommand("commandbridge")
				.withAliases("cb")
				.withPermission("commandbridge.admin")

				// /cb (default) -> help
				.executes((ResultingCommandExecutor) (sender, args) -> {
					CommandSource src = sender; // already a Velocity CommandSource
					help.execute(src);
					return 1;
				})

				// /cb help
				.withSubcommand(new CommandAPICommand("help")
						.executes((CommandExecutor) (sender, args) -> {
							CommandSource src = sender;
							help.execute(src);
						}))

				// /cb scripts
				.withSubcommand(new CommandAPICommand("scripts")
						.executes((CommandExecutor) (sender, args) -> {
							CommandSource src = sender;
							scriptsCmd.execute(src);
						}))

				// /cb reload
				.withSubcommand(new CommandAPICommand("reload")
						.executes((CommandExecutor) (sender, args) -> {
							CommandSource src = sender;
							reload.execute(src);
						}))

				// /cb list
				.withSubcommand(new CommandAPICommand("list")
						.executes((CommandExecutor) (sender, args) -> {
							CommandSource src = sender;
							list.execute(src);
						}))

				// /cb inspect <clientId>
				.withSubcommand(new CommandAPICommand("inspect")
						.withArguments(new StringArgument("clientId")
								.replaceSuggestions(clientIdSuggestions))
						.executes((CommandExecutor) (sender, args) -> {
							CommandSource src = sender;
							String id = (String) args.get("clientId");
							client.execute(src, id);
						}))

				// /cb ping
				.withSubcommand(new CommandAPICommand("ping")
						.executes((CommandExecutor) (sender, args) -> {
							CommandSource src = sender;
							ping.execute(src);
						}))

				// /cb debug
				.withSubcommand(new CommandAPICommand("debug")
						.executes((CommandExecutor) (sender, args) -> {
							CommandSource src = sender;
							debug.execute(src);
						}))

				// /cb dump
				.withSubcommand(new CommandAPICommand("dump")
						.executes((CommandExecutor) (sender, args) -> {
							CommandSource src = sender;
							dump.execute(src);
						}))

				.register();

		Log.info("Registered /commandbridge (CommandAPI, MiniMessage)");
	}
}
