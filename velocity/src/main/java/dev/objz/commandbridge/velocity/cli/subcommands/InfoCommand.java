package dev.objz.commandbridge.velocity.cli.subcommands;

import com.velocitypowered.api.command.CommandSource;
import dev.objz.commandbridge.util.MM;
import dev.objz.commandbridge.velocity.ui.cli.CliOutput;
import dev.objz.commandbridge.velocity.ui.cli.CliTable;
import dev.objz.commandbridge.velocity.ui.RenderContext;
import dev.objz.commandbridge.velocity.ui.Theme;
import dev.objz.commandbridge.velocity.ui.components.ProgressBarComponent;
import net.kyori.adventure.text.Component;

import java.lang.management.ManagementFactory;

public final class InfoCommand extends AbstractCliCommand {

    public void execute(CommandSource sender) {
        RenderContext ctx = new RenderContext(sender);
        
        if (ctx.isConsole()) {
            renderConsole();
        } else {
            renderChat(sender, ctx);
        }
    }

    private void renderChat(CommandSource sender, RenderContext ctx) {
        long uptime = ManagementFactory.getRuntimeMXBean().getUptime();
        long memUsed = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024;
        long memTotal = Runtime.getRuntime().totalMemory() / 1024 / 1024;

        double ratio = memTotal == 0 ? 0 : (double) memUsed / (double) memTotal;
        String barColor = ratio > 0.85 ? Theme.C_ERROR
                : (ratio > 0.65 ? Theme.C_WARN : Theme.C_SUCCESS);
        ProgressBarComponent bar = new ProgressBarComponent(ratio, 18, barColor);
        int percent = (int) (ratio * 100);

        Component barLine = MM.parse("<" + Theme.C_MUTED + ">Memory Load:</"
                + Theme.C_MUTED + "> ")
                .append(bar.renderChat(ctx))
                .append(MM.parse(" <" + Theme.C_MUTED + ">" + memUsed + "MB / "
                        + memTotal + "MB (" + percent + "%)</" + Theme.C_MUTED + ">"));
        String barConsole = "Memory Load: " + memUsed + "MB / " + memTotal + "MB (" + percent + "%)";

        dev.objz.commandbridge.velocity.ui.Report.of("System Info")
                .kv("OS", System.getProperty("os.name") + " (" + System.getProperty("os.arch") + ")")
                .kv("Java", System.getProperty("java.version"))
                .kv("Uptime", formatDuration(uptime))
                .kv("Memory", memUsed + "MB / " + memTotal + "MB")
                .kv("Cores", String.valueOf(Runtime.getRuntime().availableProcessors()))
                .blank()
                .line(barLine, barConsole)
                .sendChatOnly(sender);
    }

    private void renderConsole() {
        // Data
        long uptime = ManagementFactory.getRuntimeMXBean().getUptime();
        long memUsed = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024;
        long memTotal = Runtime.getRuntime().totalMemory() / 1024 / 1024;

        CliOutput output = cli("System Info");

        CliTable table = new CliTable()
                .width(output.width())
                .addColumn("Metric", CliTable.Align.LEFT, 1, 10)
                .addColumn("Value", CliTable.Align.LEFT, 4, 24);
        table.addRow("OS", System.getProperty("os.name") + " (" + System.getProperty("os.arch") + ")");
        table.addRow("Java", System.getProperty("java.version"));
        table.addRow("Uptime", formatDuration(uptime));
        table.addRow("Memory", memUsed + "MB / " + memTotal + "MB");
        table.addRow("Cores", String.valueOf(Runtime.getRuntime().availableProcessors()));

        output.appendRaw(table.render());
        log(output);
    }

    private String formatDuration(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        return String.format("%02dh %02dm %02ds", hours, minutes % 60, seconds % 60);
    }
}
