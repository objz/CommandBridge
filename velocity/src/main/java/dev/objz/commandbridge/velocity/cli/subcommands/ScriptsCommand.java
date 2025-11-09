package dev.objz.commandbridge.velocity.cli.subcommands;

import com.velocitypowered.api.command.CommandSource;
import dev.objz.commandbridge.velocity.ScriptManager;
import dev.objz.commandbridge.velocity.util.BarBuilder;
import dev.objz.commandbridge.velocity.util.MM;

public final class ScriptsCommand {
	private final ScriptManager scripts;

	public ScriptsCommand(ScriptManager scripts) {
		this.scripts = scripts;
	}

	public void execute(CommandSource sender) {
		int enabled = scripts.enabled().size();
		int disabled = scripts.disabled().size();
		int loaded = scripts.loaded().size();
		int errors = (int) scripts.errors();

		if (loaded == 0) {
			MM.msg().space().line(MM.warn("No scripts loaded")).send(sender);
			return;
		}

		double green = enabled / (double) loaded;
		double yellow = (disabled - errors) / (double) loaded;
		double red = Math.min(errors, loaded) / (double) loaded;

		String bar = BarBuilder.create(110)
				.add("green", green)
				.add("yellow", yellow)
				.add("red", red)
				.build();

		MM.msg()
				.space()
				.header("Scripts")
				.line(MM.parse(bar))
				.line(MM.kv("loaded", String.valueOf(loaded))
						.append(MM.sep())
						.append(MM.kv("enabled", "<green>" + enabled + "</green>"))
						.append(MM.sep())
						.append(MM.kv("disabled", "<yellow>" + disabled + "</yellow>"))
						.append(MM.sep())
						.append(MM.kv("errors", "<red>" + errors + "</red>")))
				.send(sender);
	}

}
