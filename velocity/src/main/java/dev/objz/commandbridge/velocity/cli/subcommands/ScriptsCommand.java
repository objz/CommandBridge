package dev.objz.commandbridge.velocity.cli.subcommands;

import com.velocitypowered.api.command.CommandSource;
import dev.objz.commandbridge.scripting.DebugPrinter;
import dev.objz.commandbridge.scripting.model.Script;
import dev.objz.commandbridge.util.MM;
import dev.objz.commandbridge.velocity.ScriptManager;
import dev.objz.commandbridge.velocity.ui.chat.ChatFrame;

import dev.objz.commandbridge.velocity.ui.cli.CliOutput;
import dev.objz.commandbridge.velocity.ui.RenderContext;
import dev.objz.commandbridge.velocity.ui.Theme;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;

public final class ScriptsCommand extends AbstractCliCommand {

    private final ScriptManager scriptManager;

    public ScriptsCommand(ScriptManager scriptManager) {
        this.scriptManager = scriptManager;
    }

    public void execute(CommandSource sender, int page) {
        RenderContext ctx = new RenderContext(sender);

        if (ctx.isPlayer()) {
            renderChatScripts(sender, page);
        } else {
            renderConsoleScripts();
        }
    }

    private void renderChatScripts(CommandSource sender, int page) {
        var scripts = scriptManager.loaded();

        if (scripts.isEmpty()) {
            Component warn = MM.warn("No scripts loaded");
            ChatFrame frame = new ChatFrame("Scripts");
            frame.line(warn);
            frame.send(sender);
            return;
        }

        int perPage = 5;
        int totalPages = (int) Math.ceil((double) scripts.size() / perPage);
        int actualPage = Math.max(1, Math.min(page, totalPages));
        int start = (actualPage - 1) * perPage;
        int end = Math.min(start + perPage, scripts.size());

        List<Component> lines = new ArrayList<>();
        for (int i = start; i < end; i++) {
            Script s = scripts.get(i);
            String statusColor = s.enabled() ? Theme.C_SUCCESS : Theme.C_ERROR;
            String statusIcon = s.enabled() ? Theme.SYMBOL_CHECK : Theme.SYMBOL_CROSS;
            String descText = s.description() != null ? s.description() : "no description";
            String aliases = s.aliases() != null && !s.aliases().isEmpty() ? String.join(", ", s.aliases()) : "none";
            int commandCount = s.commands() != null ? s.commands().size() : 0;

            Component header = MM.parse("<" + statusColor + "><bold>" + statusIcon + "</bold></" + statusColor + "> ")
                    .append(MM.parse("<gradient:" + Theme.C_ACCENT + ":" + Theme.C_PRIMARY + "><bold>" + s.name() + "</bold></gradient>"));
            Component desc = MM.parse("<" + Theme.C_MUTED + ">  " + descText + "</" + Theme.C_MUTED + ">");
            Component version = MM.parse("<" + Theme.C_MUTED + ">  Version:</" + Theme.C_MUTED + "> <white>" + s.version() + "</white>");
            Component aliasLine = MM.parse("<" + Theme.C_MUTED + ">  Aliases:</" + Theme.C_MUTED + "> <white>" + aliases + "</white>");
            Component commandsLine = MM.parse("<" + Theme.C_MUTED + ">  Commands:</" + Theme.C_MUTED + "> <white>" + commandCount + "</white>");
            Component statusLine = MM.parse("<" + Theme.C_MUTED + ">  Status:</" + Theme.C_MUTED + "> <" + statusColor + ">" + (s.enabled() ? "Enabled" : "Disabled") + "</" + statusColor + ">");

            lines.add(header);
            lines.add(desc);
            lines.add(version);
            lines.add(aliasLine);
            lines.add(commandsLine);
            lines.add(statusLine);
            if (i < end - 1) {
                lines.add(Component.empty());
            }
        }

        int loaded = scriptManager.loaded().size();
        int enabled = scriptManager.enabled().size();
        int disabled = scriptManager.disabled().size();
        long errors = scriptManager.errors();

        lines.add(Component.empty());
        lines.add(MM.parse("<" + Theme.C_MUTED + ">Loaded:</" + Theme.C_MUTED + "> <" + Theme.C_ACCENT + ">" + loaded + "</" + Theme.C_ACCENT + ">"));
        lines.add(MM.parse("<" + Theme.C_MUTED + ">Enabled:</" + Theme.C_MUTED + "> <" + Theme.C_SUCCESS + ">" + enabled + "</" + Theme.C_SUCCESS + ">"));
        lines.add(MM.parse("<" + Theme.C_MUTED + ">Disabled:</" + Theme.C_MUTED + "> <" + Theme.C_WARN + ">" + disabled + "</" + Theme.C_WARN + ">"));
        lines.add(MM.parse("<" + Theme.C_MUTED + ">Errors:</" + Theme.C_MUTED + "> <" + Theme.C_ERROR + ">" + errors + "</" + Theme.C_ERROR + ">"));

        if (totalPages > 1) {
            Component nav = Component.empty();
            if (actualPage > 1) {
                nav = nav.append(MM.parse("<" + Theme.C_ACCENT + "><bold>" + Theme.SYMBOL_ARROW_LEFT + "</bold></" + Theme.C_ACCENT + ">")
                        .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/cb scripts " + (actualPage - 1)))
                        .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(MM.parse("<" + Theme.C_MUTED + ">Previous page</" + Theme.C_MUTED + ">"))));
            } else {
                nav = nav.append(MM.parse("<" + Theme.C_SEP + ">" + Theme.SYMBOL_ARROW_LEFT + "</" + Theme.C_SEP + ">"));
            }
            nav = nav.append(MM.parse(" <" + Theme.C_MUTED + ">Page " + actualPage + "/" + totalPages + "</" + Theme.C_MUTED + "> "));
            if (actualPage < totalPages) {
                nav = nav.append(MM.parse("<" + Theme.C_ACCENT + "><bold>" + Theme.SYMBOL_ARROW_RIGHT + "</bold></" + Theme.C_ACCENT + ">")
                        .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/cb scripts " + (actualPage + 1)))
                        .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(MM.parse("<" + Theme.C_MUTED + ">Next page</" + Theme.C_MUTED + ">"))));
            } else {
                nav = nav.append(MM.parse("<" + Theme.C_SEP + ">" + Theme.SYMBOL_ARROW_RIGHT + "</" + Theme.C_SEP + ">"));
            }
            lines.add(Component.empty());
            lines.add(nav);
        }

        ChatFrame frame = new ChatFrame("Scripts");
        frame.lines(lines);
        frame.send(sender);
    }

    private void renderConsoleScripts() {
        var scripts = scriptManager.loaded();
        
        if (scripts.isEmpty()) {
            CliOutput output = cli("Scripts");
            output.warn("No scripts loaded");
            log(output);
            return;
        }
        
        // Use DebugPrinter for beautiful 2-column grid layout
        String gridOutput = DebugPrinter.printGrid(scripts);
        
        // Calculate statistics
        int loaded = scriptManager.loaded().size();
        int enabled = scriptManager.enabled().size();
        int disabled = scriptManager.disabled().size();
        long errors = scriptManager.errors();
        
        CliOutput output = cli("Scripts");
        output.appendRaw(gridOutput);
        if (!gridOutput.endsWith("\n")) {
            output.appendRaw("\n");
        }
        output.blankLine();
        output.appendRaw(Theme.ANSI_PRIMARY).appendRaw(Theme.ANSI_BOLD);
        output.appendRaw("Scripts summary: ");
        output.appendRaw(Theme.ANSI_RESET);
        output.appendRaw("loaded ").appendRaw(Theme.ANSI_ACCENT).appendRaw(String.valueOf(loaded)).appendRaw(Theme.ANSI_RESET);
        output.appendRaw(", enabled ").appendRaw(Theme.ANSI_SUCCESS).appendRaw(String.valueOf(enabled)).appendRaw(Theme.ANSI_RESET);
        output.appendRaw(", disabled ").appendRaw(Theme.ANSI_WARN).appendRaw(String.valueOf(disabled)).appendRaw(Theme.ANSI_RESET);
        output.appendRaw(", errors ").appendRaw(Theme.ANSI_ERROR).appendRaw(String.valueOf(errors)).appendRaw(Theme.ANSI_RESET);
        output.appendRaw("\n");
        log(output);
    }
}
