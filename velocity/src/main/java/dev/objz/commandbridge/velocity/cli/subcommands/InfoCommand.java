package dev.objz.commandbridge.velocity.cli.subcommands;

import com.velocitypowered.api.command.CommandSource;
import dev.objz.commandbridge.velocity.ui.CliOutput;
import dev.objz.commandbridge.velocity.ui.CliTable;
import dev.objz.commandbridge.velocity.ui.RenderContext;
import dev.objz.commandbridge.velocity.ui.components.HeaderComponent;
import dev.objz.commandbridge.velocity.ui.components.TableComponent;

import java.lang.management.ManagementFactory;

public class InfoCommand extends AbstractCliCommand {

    public void execute(CommandSource sender) {
        RenderContext ctx = new RenderContext(sender);
        
        if (ctx.isConsole()) {
            renderConsole();
        } else {
            renderChat(sender, ctx);
        }
    }

    private void renderChat(CommandSource sender, RenderContext ctx) {
        sender.sendMessage(new HeaderComponent("System Info").renderChat(ctx));

        // Data
        long uptime = ManagementFactory.getRuntimeMXBean().getUptime();
        long memUsed = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024;
        long memTotal = Runtime.getRuntime().totalMemory() / 1024 / 1024;
        
        TableComponent table = new TableComponent("Metric", "Value");
        table.addRow("OS", System.getProperty("os.name") + " (" + System.getProperty("os.arch") + ")");
        table.addRow("Java", System.getProperty("java.version"));
        table.addRow("Uptime", formatDuration(uptime));
        table.addRow("Memory", memUsed + "MB / " + memTotal + "MB");
        table.addRow("Cores", String.valueOf(Runtime.getRuntime().availableProcessors()));

        sender.sendMessage(table.renderChat(ctx));
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
