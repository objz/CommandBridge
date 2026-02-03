package dev.objz.commandbridge.velocity.cli.subcommands;

import com.velocitypowered.api.command.CommandSource;
import dev.objz.commandbridge.security.AuthStatus;
import dev.objz.commandbridge.velocity.net.session.ClientSession;
import dev.objz.commandbridge.velocity.net.session.SessionHub;
import dev.objz.commandbridge.velocity.ui.CliOutput;
import dev.objz.commandbridge.velocity.ui.CliTable;
import dev.objz.commandbridge.velocity.ui.RenderContext;
import dev.objz.commandbridge.velocity.ui.components.HeaderComponent;
import dev.objz.commandbridge.velocity.ui.components.TableComponent;

import java.util.ArrayList;
import java.util.List;

public class ListCommand extends AbstractCliCommand {
    private final SessionHub sessions;

    public ListCommand(SessionHub sessions) {
        this.sessions = sessions;
    }

    public void execute(CommandSource sender) {
        RenderContext ctx = new RenderContext(sender);
        
        if (ctx.isConsole()) {
            renderConsole();
        } else {
            renderChat(sender, ctx);
        }
    }

    private void renderChat(CommandSource sender, RenderContext ctx) {
        sender.sendMessage(new HeaderComponent("Clients").renderChat(ctx));

        List<ClientSession> authenticated = getAuthenticatedClients();

        if (authenticated.isEmpty()) {
            sender.sendMessage(dev.objz.commandbridge.util.MM.warn("No authenticated clients connected"));
            return;
        }

        TableComponent table = new TableComponent("ID", "Address", "Platform");

        for (var s : authenticated) {
            String id = s.id() != null ? s.id() : "unknown";
            String address = s.ch() != null && s.ch().getSourceAddress() != null
                    ? s.ch().getSourceAddress().toString()
                    : "unknown";
            String platform = "Bukkit";

            table.addRow(id, address, platform);
        }

        sender.sendMessage(table.renderChat(ctx));
    }

    private void renderConsole() {
        List<ClientSession> authenticated = getAuthenticatedClients();

        CliOutput output = cli("Clients");

        if (authenticated.isEmpty()) {
            output.warn("No authenticated clients connected");
            log(output);
            return;
        }

        CliTable table = new CliTable()
                .width(output.width())
                .addColumn("ID", CliTable.Align.LEFT, 2, 8)
                .addColumn("Address", CliTable.Align.LEFT, 5, 18)
                .addColumn("Platform", CliTable.Align.LEFT, 1, 8);

        for (var s : authenticated) {
            String id = s.id() != null ? s.id() : "unknown";
            String address = s.ch() != null && s.ch().getSourceAddress() != null
                    ? s.ch().getSourceAddress().toString()
                    : "unknown";
            String platform = s.location() != null ? s.location().name() : "unknown";
            table.addRow(id, address, platform);
        }

        output.appendRaw(table.render());
        log(output);
    }

    private List<ClientSession> getAuthenticatedClients() {
        List<ClientSession> authenticated = new ArrayList<>();
        for (ClientSession s : sessions) {
            if (s.status() == AuthStatus.AUTH_OK && s.ch() != null && s.ch().isOpen()) {
                authenticated.add(s);
            }
        }
        return authenticated;
    }
}
