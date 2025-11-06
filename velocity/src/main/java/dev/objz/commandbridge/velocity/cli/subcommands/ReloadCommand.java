package dev.objz.commandbridge.velocity.cli.subcommands;

import com.velocitypowered.api.command.CommandSource;
import dev.objz.commandbridge.config.ConfigManager;
import dev.objz.commandbridge.config.model.VelocityConfig;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.velocity.RegistrationManager;
import dev.objz.commandbridge.velocity.ScriptManager;
import dev.objz.commandbridge.velocity.util.MM;

public final class ReloadCommand {

	private final ConfigManager configManager;
	private final ScriptManager scriptManager;
	private final RegistrationManager registrationManager;

	public ReloadCommand(ConfigManager configManager, ScriptManager scriptManager,
			RegistrationManager registrationManager) {
		this.configManager = configManager;
		this.scriptManager = scriptManager;
		this.registrationManager = registrationManager;
	}

	public void execute(CommandSource sender) {
		long start = System.currentTimeMillis();

		try {
			boolean configOk = configManager.reload(VelocityConfig.class);
			VelocityConfig cfg = configManager.current(VelocityConfig.class);
			Log.setDebug(cfg.debug());

			scriptManager.loadAll();
			registrationManager.load(scriptManager.enabled());

			long elapsed = System.currentTimeMillis() - start;

			MM.msg()
					.line(MM.ok("Reload complete"))
					.line(MM.sep().append(MM.kv("time", elapsed + "ms"))
							.append(MM.sep())
							.append(MM.kv("enabled",
									String.valueOf(scriptManager.enabled().size())))
							.append(MM.sep())
							.append(MM.kv("disabled", String.valueOf(scriptManager.all()
									.size() - scriptManager.enabled().size())))
							.append(MM.sep())
							.append(MM.kv("errors",
									String.valueOf(scriptManager.totalErrors()))))
					.send(sender);

			if (!configOk) {
				MM.msg().line(MM.warn("Config reloaded with warnings")).send(sender);
			}

		} catch (Exception e) {
			Log.error(e, "Reload failed");
			MM.msg().line(MM.error("Reload failed: " + e.getMessage())).send(sender);
		}
	}
}
