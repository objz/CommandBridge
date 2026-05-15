package dev.objz.commandbridge.velocity.cli.subcommands;

import com.velocitypowered.api.command.CommandSource;
import dev.objz.commandbridge.util.MM;
import dev.objz.commandbridge.velocity.ui.cli.BoxDrawing;
import dev.objz.commandbridge.velocity.ui.cli.CliLayout;
import dev.objz.commandbridge.velocity.ui.RenderContext;
import dev.objz.commandbridge.velocity.ui.Theme;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

public final class HelpCommand extends AbstractCliCommand {

    private static final String[][] COMMANDS = {
        {"/cb help", "Shows this help menu"},
        {"/cb info", "System & Plugin information"},
        {"/cb reload", "Reload configuration & scripts"},
        {"/cb debug", "Toggle debug mode"},
        {"/cb dump", "Dump configuration for support"},
        {"/cb migrate", "Migrate scripts to latest version"},
        {"/cb script list", "List loaded scripts"},
        {"/cb script show", "Show a script (optionally a group)"},
        {"/cb script enable", "Enable a script"},
        {"/cb script disable", "Disable a script"},
        {"/cb task list", "List pending scheduled tasks"},
        {"/cb task clear", "Clear a pending task"},
        {"/cb client list", "List connected clients"},
        {"/cb client ping", "Ping client(s)"},
        {"/cb client players", "Players on a client"},
        {"/cb config show", "Show config (optionally a section)"},
        {"/cb config reload", "Reload only config"}
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
        dev.objz.commandbridge.velocity.ui.Report report =
                dev.objz.commandbridge.velocity.ui.Report.of("CommandBridge Help");
        for (String[] cmd : COMMANDS) {
            String command = cmd[0];
            String description = cmd[1];
            Component clickable = MM.cmd(command)
                    .clickEvent(ClickEvent.suggestCommand(command))
                    .hoverEvent(HoverEvent.showText(MM.muted("Click to insert")));
            report.bullet(clickable, command);
            report.line(MM.muted("  " + description), "  " + description);
        }
        report.sendChatOnly(sender);
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
