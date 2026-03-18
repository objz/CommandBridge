package dev.objz.commandbridge.velocity.cli.subcommands;

import com.velocitypowered.api.command.CommandSource;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.util.MM;
import dev.objz.commandbridge.velocity.ui.chat.ChatFrame;

import dev.objz.commandbridge.velocity.ui.cli.CliOutput;
import dev.objz.commandbridge.velocity.ui.RenderContext;
import dev.objz.commandbridge.velocity.ui.Theme;
import net.kyori.adventure.text.Component;

public final class DebugCommand extends AbstractCliCommand {
    public void execute(CommandSource sender) {
        boolean newState = !Log.isDebug();
        Log.setDebug(newState);
        
        RenderContext ctx = new RenderContext(sender);
        
        if (ctx.isPlayer()) {
            renderChat(ctx, newState);
        } else {
            renderConsole(newState);
        }
    }
    
    private void renderChat(RenderContext ctx, boolean enabled) {
        String status = enabled ? "ENABLED" : "DISABLED";
        String color = enabled ? Theme.C_SUCCESS : Theme.C_WARN;
        Component statusComp = MM.parse("<" + Theme.C_MUTED + ">Debug Mode</" + Theme.C_MUTED + "> <" + color + "><bold>" + status + "</bold></" + color + ">");
        Component toggle = MM.parse(" <" + Theme.C_ACCENT + "><bold>[Toggle]</bold></" + Theme.C_ACCENT + ">")
                .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/cb debug"))
                .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(
                        MM.parse("<" + Theme.C_MUTED + ">Click to toggle debug</" + Theme.C_MUTED + ">")));
        Component line = statusComp.append(toggle);
        ChatFrame frame = new ChatFrame("Debug");
        frame.line(line);
        frame.send(ctx.source());
    }
    
    private void renderConsole(boolean enabled) {
        CliOutput output = cli("Debug Mode");
        String status = enabled ? "ENABLED" : "DISABLED";
        String statusColor = enabled ? Theme.ANSI_SUCCESS : Theme.ANSI_WARN;
        
        output.appendRaw("Debug is now ");
        output.appendRaw(statusColor).appendRaw(Theme.ANSI_BOLD);
        output.appendRaw(status);
        output.appendRaw(Theme.ANSI_RESET).appendRaw("\n");
        
        log(output);
    }
}
