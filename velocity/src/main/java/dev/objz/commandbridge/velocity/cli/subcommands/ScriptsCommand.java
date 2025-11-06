package dev.objz.commandbridge.velocity.cli.subcommands;

import com.velocitypowered.api.command.CommandSource;

import dev.objz.commandbridge.scripting.model.Script;
import dev.objz.commandbridge.velocity.ScriptManager;
import dev.objz.commandbridge.velocity.util.MM;

import java.util.List;

public final class ScriptsCommand {
	private final ScriptManager scripts;

	public ScriptsCommand(ScriptManager scripts) {
		this.scripts = scripts;
	}

	public void execute(CommandSource sender) {
		List<Script> all = scripts.all();
		List<Script> enabled = scripts.enabled();
		long errors = scripts.totalErrors();

		if (all.isEmpty()) {
			MM.msg().line(MM.warn("No scripts loaded")).send(sender);
			return;
		}

		var m = MM.msg()
				.header("Scripts")
				.kv("enabled", String.valueOf(enabled.size()))
				.kv("disabled", String.valueOf(all.size() - enabled.size()))
				.kv("errors", String.valueOf(errors));

		if (!enabled.isEmpty()) {
			m.header("Enabled");
			for (var s : enabled) {
				String desc = s.description() != null ? s.description() : "no description";
				m.item("<green>" + s.name() + "</green> v" + s.version())
						.line(MM.sep().append(MM.desc(desc))); // stay on separate line? If you
											// prefer same line, replace
											// with m.item( ... + " " + )
			}
		}

		var disabled = all.stream().filter(s -> !s.enabled()).toList();
		if (!disabled.isEmpty()) {
			m.header("Disabled");
			for (var s : disabled) {
				m.item("<gray>" + s.name() + "</gray> v" + s.version());
			}
		}

		m.send(sender);
	}
}
