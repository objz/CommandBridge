package dev.objz.commandbridge.velocity.cli.subcommands;

import com.velocitypowered.api.command.CommandSource;
import dev.objz.commandbridge.util.MM;
import dev.objz.commandbridge.velocity.ui.chat.ChatFrame;
import dev.objz.commandbridge.velocity.ui.chat.ChatLayout;
import java.util.ArrayList;
import java.util.List;
import dev.objz.commandbridge.velocity.ui.cli.BoxDrawing;
import dev.objz.commandbridge.velocity.ui.cli.CliLayout;
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
        List<Component> lines = new ArrayList<>();
        for (String[] cmd : COMMANDS) {
            String command = cmd[0];
            String description = cmd[1];

            Component commandComp = MM.cmd(command)
                    .hoverEvent(HoverEvent.showText(MM.parse("<" + Theme.C_MUTED + ">Run command</" + Theme.C_MUTED + ">")))
                    .clickEvent(ClickEvent.runCommand(command));
            Component line = MM.parse("<" + Theme.C_ACCENT + ">•</" + Theme.C_ACCENT + "> ").append(commandComp);
            Component desc = MM.parse("<" + Theme.C_MUTED + ">  " + description + "</" + Theme.C_MUTED + ">");

            lines.add(line);
            lines.add(desc);
        }

        int width = ChatLayout.titleWidth("CommandBridge Help");
        for (Component line : lines) {
            width = Math.max(width, ChatLayout.visibleLength(line));
        }
        width = Math.max(width, ChatLayout.DEFAULT_WIDTH_PX);

        ChatFrame frame = new ChatFrame("CommandBridge Help")
                .width(width);
        frame.lines(lines);
        frame.send(sender);
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
