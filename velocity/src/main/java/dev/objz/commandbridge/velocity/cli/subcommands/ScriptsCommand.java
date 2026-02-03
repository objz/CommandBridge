package dev.objz.commandbridge.velocity.cli.subcommands;

import com.velocitypowered.api.command.CommandSource;
import dev.objz.commandbridge.scripting.DebugPrinter;
import dev.objz.commandbridge.scripting.model.Script;
import dev.objz.commandbridge.util.MM;
import dev.objz.commandbridge.velocity.ScriptManager;
import dev.objz.commandbridge.velocity.ui.CliOutput;
import dev.objz.commandbridge.velocity.ui.RenderContext;
import dev.objz.commandbridge.velocity.ui.Theme;
import dev.objz.commandbridge.velocity.ui.components.PagedList;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;

public class ScriptsCommand extends AbstractCliCommand {

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
        
        // Header
        Component header = MM.parse(
            "\n<gradient:" + Theme.C_PRIMARY + ":" + Theme.C_ACCENT + "><bold>" +
            "━━━━ SCRIPTS ━━━━" +
            "</bold></gradient>\n"
        );
        sender.sendMessage(header);

        if (scripts.isEmpty()) {
            sender.sendMessage(MM.warn("No scripts loaded"));
            return;
        }

        PagedList<Script> list = new PagedList<>(
            scripts,
            // Chat Renderer
            (s) -> {
                String statusColor = s.enabled() ? Theme.C_SUCCESS : Theme.C_ERROR;
                String statusIcon = s.enabled() ? Theme.SYMBOL_CHECK : Theme.SYMBOL_CROSS;
                
                Component bullet = MM.parse(" <" + statusColor + "><bold>" + statusIcon + "</bold></" + statusColor + "> ");
                Component name = MM.parse("<gradient:" + Theme.C_ACCENT + ":" + Theme.C_PRIMARY + "><bold>" + s.name() + "</bold></gradient>");
                Component desc = s.description() != null 
                    ? MM.parse(" <" + Theme.C_SEP + ">━</" + Theme.C_SEP + "> <" + Theme.C_MUTED + ">" + s.description() + "</" + Theme.C_MUTED + ">") 
                    : Component.empty();
                
                return bullet
                        .append(name)
                        .append(desc)
                        .hoverEvent(HoverEvent.showText(
                                MM.parse("<gradient:" + Theme.C_PRIMARY + ":" + Theme.C_ACCENT + "><bold>" + s.name() + "</bold></gradient>\n\n" +
                                         "<" + Theme.C_ACCENT + ">Version:</" + Theme.C_ACCENT + "> <white>" + s.version() + "</white>\n" +
                                         "<" + Theme.C_ACCENT + ">Aliases:</" + Theme.C_ACCENT + "> <white>" + (s.aliases() != null && !s.aliases().isEmpty() ? String.join(", ", s.aliases()) : "none") + "</white>\n" +
                                         "<" + Theme.C_ACCENT + ">Commands:</" + Theme.C_ACCENT + "> <white>" + (s.commands() != null ? s.commands().size() : "0") + "</white>\n" +
                                         "<" + Theme.C_ACCENT + ">Status:</" + Theme.C_ACCENT + "> " + (s.enabled() ? "<" + Theme.C_SUCCESS + ">Enabled</" + Theme.C_SUCCESS + ">" : "<" + Theme.C_ERROR + ">Disabled</" + Theme.C_ERROR + ">"))
                        ));
            },
            // Console Renderer
            (s) -> {
                String color = s.enabled() ? Theme.ANSI_SUCCESS : Theme.ANSI_ERROR;
                String icon = s.enabled() ? "+" : "-";
                String desc = s.description() != null ? s.description() : "";
                return String.format("%s%s%s %-20s %s%s%s", 
                    color, icon, Theme.ANSI_RESET,
                    s.name(), 
                    Theme.ANSI_MUTED, desc, Theme.ANSI_RESET);
            },
            page,
            10,
            "/cb scripts"
        );

        RenderContext ctx = new RenderContext(sender);
        sender.sendMessage(list.renderChat(ctx));
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
