package dev.objz.commandbridge.velocity.cli.subcommands;

import com.velocitypowered.api.command.CommandSource;
import dev.objz.commandbridge.security.AuthStatus;
import dev.objz.commandbridge.velocity.net.session.ClientSession;
import dev.objz.commandbridge.velocity.net.session.SessionHub;
import dev.objz.commandbridge.util.MM;
import dev.objz.commandbridge.velocity.ui.chat.ChatFrame;

import java.util.ArrayList;
import java.util.List;
import dev.objz.commandbridge.velocity.ui.cli.CliOutput;
import dev.objz.commandbridge.velocity.ui.cli.CliTable;
import dev.objz.commandbridge.velocity.ui.RenderContext;
import dev.objz.commandbridge.velocity.ui.Theme;
import net.kyori.adventure.text.Component;

public final class ListCommand extends AbstractCliCommand {
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
        List<ClientSession> authenticated = getAuthenticatedClients();

        if (authenticated.isEmpty()) {
            Component warn = MM.warn("No authenticated clients connected");
            ChatFrame frame = new ChatFrame("Clients");
            frame.line(warn);
            frame.send(sender);
            return;
        }

        List<Component> lines = new ArrayList<>();
        Component summary = MM.parse("<" + Theme.C_MUTED + ">Authenticated clients</" + Theme.C_MUTED + "> <" + Theme.C_ACCENT + ">" + authenticated.size() + "</" + Theme.C_ACCENT + ">");
        lines.add(summary);

        int index = 0;
        for (var s : authenticated) {
            String id = s.id() != null ? s.id() : "unknown";
            String address = s.endpoint() != null ? s.endpoint().describe() : "unknown";
            String platform = s.location() != null ? s.location().name() : "unknown";

            Component header = MM.parse("<" + Theme.C_ACCENT + ">•</" + Theme.C_ACCENT + "> ")
                    .append(MM.parse("<gradient:" + Theme.C_PRIMARY + ":" + Theme.C_ACCENT + "><bold>" + id + "</bold></gradient>"))
                    .append(MM.parse(" <" + Theme.C_MUTED + ">(" + platform + ")</" + Theme.C_MUTED + ">"));
            Component addr = MM.parse("<" + Theme.C_MUTED + ">  Address:</" + Theme.C_MUTED + "> <white>" + address + "</white>");

            lines.add(header);
            lines.add(addr);
            if (index < authenticated.size() - 1) {
                lines.add(Component.empty());
            }
            index++;
        }

        ChatFrame frame = new ChatFrame("Clients");
        frame.lines(lines);
        frame.send(sender);
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
            String address = s.endpoint() != null ? s.endpoint().describe() : "unknown";
            String platform = s.location() != null ? s.location().name() : "unknown";
            table.addRow(id, address, platform);
        }

        output.appendRaw(table.render());
        log(output);
    }

    private List<ClientSession> getAuthenticatedClients() {
        List<ClientSession> authenticated = new ArrayList<>();
        for (ClientSession s : sessions) {
            if (s.status() == AuthStatus.AUTH_OK && s.endpoint() != null && s.endpoint().isOpen()) {
                authenticated.add(s);
            }
        }
        return authenticated;
    }
}
