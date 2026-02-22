package dev.objz.commandbridge.velocity.cli.subcommands;

import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.velocity.ui.cli.CliOutput;

public abstract class AbstractCliCommand {
    protected CliOutput cli(String title) {
        return CliOutput.create(title).blankLine();
    }

    protected void log(CliOutput output) {
        Log.info(output.build());
    }
}
