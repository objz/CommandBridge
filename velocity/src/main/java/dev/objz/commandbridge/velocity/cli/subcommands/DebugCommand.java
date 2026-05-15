package dev.objz.commandbridge.velocity.cli.subcommands;

import com.velocitypowered.api.command.CommandSource;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.velocity.ui.RenderContext;
import dev.objz.commandbridge.velocity.ui.Report;
import dev.objz.commandbridge.velocity.ui.Report.Action;
import dev.objz.commandbridge.velocity.ui.Report.Status;
import dev.objz.commandbridge.velocity.ui.Theme;
import dev.objz.commandbridge.velocity.ui.cli.CliOutput;

public final class DebugCommand extends AbstractCliCommand {

    private static final String SECTION = "Debug";

    public void execute(CommandSource sender) {
        boolean newState = !Log.isDebug();
        Log.setDebug(newState);

        if (new RenderContext(sender).isPlayer()) {
            renderChat(sender, newState);
        } else {
            renderConsole(newState);
        }
    }

    private void renderChat(CommandSource sender, boolean enabled) {
        String label = enabled ? "ENABLED" : "DISABLED";
        Status status = enabled ? Status.SUCCESS : Status.WARN;

        Report report = Report.of(SECTION)
                .kv("Debug Mode", label, status)
                .blank()
                .actions(Action.of("Toggle", "/cb debug", "toggle debug mode"));
        report.sendChatOnly(sender);
    }

    private void renderConsole(boolean enabled) {
        CliOutput output = cli(SECTION);
        String status = enabled ? "ENABLED" : "DISABLED";
        String color = enabled ? Theme.ANSI_SUCCESS : Theme.ANSI_WARN;
        output.appendRaw("Debug is now ").appendRaw(color).appendRaw(Theme.ANSI_BOLD)
                .appendRaw(status).appendRaw(Theme.ANSI_RESET).appendRaw("\n");
        log(output);
    }
}
