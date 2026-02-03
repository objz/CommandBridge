package dev.objz.commandbridge.velocity.ui.components;

import dev.objz.commandbridge.util.MM;
import dev.objz.commandbridge.velocity.ui.RenderContext;
import dev.objz.commandbridge.velocity.ui.Theme;
import dev.objz.commandbridge.velocity.ui.UIComponent;
import net.kyori.adventure.text.Component;

public class ProgressBarComponent implements UIComponent {

    private final double value; 
    private final int width;
    private final String color;

    public ProgressBarComponent(double value, int width, String color) {
        this.value = Math.max(0, Math.min(1.0, value));
        this.width = width;
        this.color = color;
    }

    @Override
    public Component renderChat(RenderContext ctx) {
        int filled = (int) (width * value);
        int empty = width - filled;
        return MM.parse("<" + color + ">" + "█".repeat(filled) + "</" + color + "><" + Theme.C_MUTED + ">" + "░".repeat(empty) + "</" + Theme.C_MUTED + ">");
    }

    @Override
    public String renderConsole(RenderContext ctx) {
        // ANSI Version
        int filled = (int) (width * value);
        int empty = width - filled;
        
        String ansiColor = Theme.ANSI_PRIMARY;
        if (color.equals(Theme.C_SUCCESS)) ansiColor = Theme.ANSI_SUCCESS;
        else if (color.equals(Theme.C_WARN)) ansiColor = Theme.ANSI_WARN;
        else if (color.equals(Theme.C_ERROR)) ansiColor = Theme.ANSI_ERROR;

        return ansiColor + "=".repeat(filled) + Theme.ANSI_MUTED + "-".repeat(empty) + Theme.ANSI_RESET;
    }
}
