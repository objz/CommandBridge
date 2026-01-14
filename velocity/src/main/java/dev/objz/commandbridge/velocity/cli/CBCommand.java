package dev.objz.commandbridge.velocity.cli;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
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

import java.util.ArrayList;
import java.util.List;

public final class CBCommand {

	private final ConfigManager configManager;
	private final ScriptManager scriptManager;
	private final RegistrationManager registrationManager;
	private final SessionHub sessionHub;
	private final OutNode<Object> outNode;
	private final VelocityConfig config;

	public CBCommand(
			ConfigManager configManager,
			ScriptManager scriptManager,
			RegistrationManager registrationManager,
			SessionHub sessionHub,
			OutNode<Object> outNode,
			VelocityConfig config) {

		this.configManager = configManager;
		this.scriptManager = scriptManager;
		this.registrationManager = registrationManager;
		this.sessionHub = sessionHub;
		this.outNode = outNode;
		this.config = config;
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
		var dump = new DumpCommand(registrationManager, sessionHub);

		new CommandAPICommand("commandbridge")
				.withAliases("cb")
				.withPermission("commandbridge.admin")

				// /cb -> help
				.executes((ResultingCommandExecutor) (sender, args) -> {
					CommandSource src = sender;
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
							scripts.execute(src);
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

				// /cb ping
				.withSubcommand(new CommandAPICommand("ping")
						.withOptionalArguments(new StringArgument("clientId")
								.replaceSuggestions(clientIdSuggestions))
						.executes((CommandExecutor) (sender, args) -> {
							CommandSource src = sender;
							String id = (String) args.get("clientId");
							ping.execute(src, id);
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
	}
}
