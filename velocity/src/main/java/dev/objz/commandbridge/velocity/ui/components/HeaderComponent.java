package dev.objz.commandbridge.velocity.ui.components;

import dev.objz.commandbridge.velocity.ui.chat.ChatLayout;
import dev.objz.commandbridge.velocity.ui.cli.CliLayout;
import dev.objz.commandbridge.velocity.ui.RenderContext;
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
        Component titleComp = ChatLayout.headerTitle(title);
        Component underline = ChatLayout.headerUnderline();
        return Component.empty()
                .append(Component.newline())
                .append(titleComp)
                .append(Component.newline())
                .append(underline)
                .append(Component.newline());
    }

    @Override
    public String renderConsole(RenderContext ctx) {
        StringBuilder sb = new StringBuilder();
        CliLayout.appendHeader(sb, title, width);
        return sb.toString();
    }
}
