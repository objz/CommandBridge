package dev.objz.commandbridge.velocity.cli.subcommands;

import com.velocitypowered.api.command.CommandSource;
import dev.objz.commandbridge.util.MM;
import dev.objz.commandbridge.velocity.ui.BoxDrawing;
import dev.objz.commandbridge.velocity.ui.CliLayout;
import dev.objz.commandbridge.velocity.ui.RenderContext;
import dev.objz.commandbridge.velocity.ui.Theme;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

public class HelpCommand extends AbstractCliCommand {

    private static final String[][] COMMANDS = {
        {"/cb help", "Shows this help menu"},
        {"/cb list", "List connected proxy clients"},
        {"/cb scripts", "Manage and view scripts"},
        {"/cb ping", "Check latency of clients"},
        {"/cb info", "System & Plugin Information"},
        {"/cb dump", "Dump configuration for support"},
        {"/cb reload", "Reload configuration & scripts"},
        {"/cb debug", "Toggle debug mode"}
    };

    public void execute(CommandSource sender) {
        RenderContext ctx = new RenderContext(sender);
        
        if (ctx.isPlayer()) {
            renderChatHelp(sender);
        } else {
            renderConsoleHelp();
        }
    }

    private void renderChatHelp(CommandSource sender) {
        // Stunning header
        Component header = MM.parse(
            "\n<gradient:" + Theme.C_PRIMARY + ":" + Theme.C_ACCENT + "><bold>" +
            "▆▆▆▆▆▆▆▆▆▆▆▆▆▆▆▆▆▆▆▆▆▆▆▆▆▆▆▆\n" +
            "  COMMANDBRIDGE\n" +
            "▆▆▆▆▆▆▆▆▆▆▆▆▆▆▆▆▆▆▆▆▆▆▆▆▆▆▆▆" +
            "</bold></gradient>\n"
        );
        sender.sendMessage(header);

        // Command list with beautiful formatting
        for (String[] cmd : COMMANDS) {
            String command = cmd[0];
            String description = cmd[1];
            
            Component bullet = MM.parse("<gradient:" + Theme.C_PRIMARY + ":" + Theme.C_ACCENT + ">▶</gradient>");
            Component commandComp = MM.parse("<gradient:" + Theme.C_ACCENT + ":" + Theme.C_PRIMARY + "><bold>" + command + "</bold></gradient>")
                .clickEvent(ClickEvent.suggestCommand(command))
                .hoverEvent(HoverEvent.showText(MM.parse(
                    "<gradient:" + Theme.C_PRIMARY + ":" + Theme.C_ACCENT + ">Click to use</gradient>\n" +
                    "<" + Theme.C_MUTED + ">" + description + "</" + Theme.C_MUTED + ">"
                )));
            Component sep = MM.parse(" <" + Theme.C_SEP + ">━</" + Theme.C_SEP + "> ");
            Component desc = MM.parse("<" + Theme.C_MUTED + ">" + description + "</" + Theme.C_MUTED + ">");
            
            Component line = Component.text(" ")
                .append(bullet)
                .append(Component.text(" "))
                .append(commandComp)
                .append(sep)
                .append(desc);
            
            sender.sendMessage(line);
        }

        // Footer
        Component footer = MM.parse(
            "\n<center><gradient:" + Theme.C_PRIMARY + ":" + Theme.C_ACCENT + ">━━━━━━━━━━━━━━━━━━━━━━━</gradient></center>\n" +
            "<center><" + Theme.C_MUTED + "><i>Hover for details • Click to use</i></" + Theme.C_MUTED + "></center>\n"
        );
        sender.sendMessage(footer);
    }

    private void renderConsoleHelp() {
        int width = CliLayout.DEFAULT_WIDTH;
        
        // Build ENTIRE output as one string
        StringBuilder sb = new StringBuilder();
        
        // Top border with title
        sb.append("\n").append(Theme.ANSI_PRIMARY).append(Theme.ANSI_BOLD);
        sb.append(BoxDrawing.DOUBLE_TOP_LEFT);
        sb.append(BoxDrawing.DOUBLE_HORIZONTAL.repeat(width - 2));
        sb.append(BoxDrawing.DOUBLE_TOP_RIGHT);
        sb.append(Theme.ANSI_RESET).append("\n");
        
        // Title
        String title = " COMMANDBRIDGE - HELP ";
        int totalPadding = width - title.length() - 2;
        int leftPad = totalPadding / 2;
        int rightPad = totalPadding - leftPad;
        
        sb.append(Theme.ANSI_PRIMARY).append(BoxDrawing.DOUBLE_VERTICAL);
        sb.append(Theme.ANSI_ACCENT).append(Theme.ANSI_BOLD);
        sb.append(" ".repeat(leftPad)).append(title).append(" ".repeat(rightPad));
        sb.append(Theme.ANSI_PRIMARY).append(BoxDrawing.DOUBLE_VERTICAL);
        sb.append(Theme.ANSI_RESET).append("\n");
        
        // Middle separator
        sb.append(Theme.ANSI_PRIMARY);
        sb.append(BoxDrawing.DOUBLE_T_RIGHT);
        sb.append(BoxDrawing.DOUBLE_HORIZONTAL.repeat(width - 2));
        sb.append(BoxDrawing.DOUBLE_T_LEFT);
        sb.append(Theme.ANSI_RESET).append("\n");
        
        // Commands - calculate max content for proper alignment
        int maxCmdLen = 0;
        for (String[] cmd : COMMANDS) {
            maxCmdLen = Math.max(maxCmdLen, cmd[0].length());
        }
        
        for (String[] cmd : COMMANDS) {
            String command = cmd[0];
            String description = cmd[1];
            
            sb.append(Theme.ANSI_PRIMARY).append(BoxDrawing.DOUBLE_VERTICAL);
            sb.append(Theme.ANSI_RESET).append("  ");
            sb.append(Theme.ANSI_ACCENT).append(Theme.ANSI_BOLD);
            sb.append(String.format("%-" + maxCmdLen + "s", command));
            sb.append(Theme.ANSI_RESET).append("  ");
            sb.append(Theme.ANSI_MUTED);
            sb.append(description);
            
            // Calculate right padding dynamically
            int contentLen = 2 + maxCmdLen + 2 + description.length();
            int remainingSpace = width - contentLen - 2; // -2 for borders
            if (remainingSpace > 0) sb.append(" ".repeat(remainingSpace));
            
            sb.append(Theme.ANSI_RESET);
            sb.append(Theme.ANSI_PRIMARY).append(BoxDrawing.DOUBLE_VERTICAL);
            sb.append(Theme.ANSI_RESET).append("\n");
        }
        
        // Bottom border
        sb.append(Theme.ANSI_PRIMARY);
        sb.append(BoxDrawing.DOUBLE_BOTTOM_LEFT);
        sb.append(BoxDrawing.DOUBLE_HORIZONTAL.repeat(width - 2));
        sb.append(BoxDrawing.DOUBLE_BOTTOM_RIGHT);
        sb.append(Theme.ANSI_RESET);
        
        // Output EVERYTHING as ONE log entry
        dev.objz.commandbridge.logging.Log.info(sb.toString());
    }
}
