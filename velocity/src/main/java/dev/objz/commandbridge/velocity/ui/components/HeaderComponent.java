package dev.objz.commandbridge.velocity.ui.components;

import dev.objz.commandbridge.util.MM;
import dev.objz.commandbridge.velocity.ui.CliLayout;
import dev.objz.commandbridge.velocity.ui.RenderContext;
import dev.objz.commandbridge.velocity.ui.Theme;
import dev.objz.commandbridge.velocity.ui.UIComponent;
import net.kyori.adventure.text.Component;

public class HeaderComponent implements UIComponent {

    private final String title;
    private final int width;

    public HeaderComponent(String title) {
        this(title, CliLayout.DEFAULT_WIDTH);
    }

    public HeaderComponent(String title, int width) {
        this.title = title;
        this.width = width;
    }

    @Override
    public Component renderChat(RenderContext ctx) {
        String gradient = "<gradient:" + Theme.C_PRIMARY + ":" + Theme.C_ACCENT + ">";
        String titleText = gradient + "<bold>" + title.toUpperCase() + "</bold></gradient>";

        String underline = "<" + Theme.C_SEP + "><st>" + " ".repeat(title.length() + 10) + "</st></" + Theme.C_SEP
                + ">";

        return MM.parse("\n " + titleText + "\n " + underline + "\n");
    }

    @Override
    public String renderConsole(RenderContext ctx) {
        StringBuilder sb = new StringBuilder();
        CliLayout.appendHeader(sb, title, width);
        return sb.toString();
    }
}
