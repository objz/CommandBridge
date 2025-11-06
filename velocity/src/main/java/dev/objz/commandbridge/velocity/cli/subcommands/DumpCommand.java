package dev.objz.commandbridge.velocity.cli.subcommands;

import com.velocitypowered.api.command.CommandSource;
import dev.objz.commandbridge.velocity.RegistrationManager;
import dev.objz.commandbridge.velocity.net.session.ClientSession;
import dev.objz.commandbridge.velocity.net.session.SessionHub;
import dev.objz.commandbridge.velocity.util.MM;

import java.util.ArrayList;
import java.util.List;

public final class DumpCommand {

	private final RegistrationManager registrations;
	private final SessionHub sessions;

	public DumpCommand(RegistrationManager registrations, SessionHub sessions) {
		this.registrations = registrations;
		this.sessions = sessions;
	}

	public void execute(CommandSource sender) {
		List<ClientSession> list = new ArrayList<>();
		for (ClientSession s : sessions)
			list.add(s);

		MM.msg()
				.header("Dump")
				.kv("clients", String.valueOf(list.size()))
				.line(MM.warn("// TODO: detailed registration . dump"))
				.send(sender);
	}
}
