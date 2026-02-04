package dev.objz.commandbridge.velocity.ui.components;

import dev.objz.commandbridge.util.MM;
import dev.objz.commandbridge.velocity.ui.RenderContext;
import dev.objz.commandbridge.velocity.ui.Theme;
import dev.objz.commandbridge.velocity.ui.UIComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

import java.util.List;
import java.util.function.Function;

public class PagedList<T> implements UIComponent {

    private final List<T> items;
    private final Function<T, Component> chatRenderer;
    private final Function<T, String> consoleRenderer;
    private final int page;
    private final int perPage;
    private final String commandPrefix;

    public PagedList(List<T> items, Function<T, Component> chatRenderer, Function<T, String> consoleRenderer, int page,
            int perPage, String commandPrefix) {
        this.items = items;
        this.chatRenderer = chatRenderer;
        this.consoleRenderer = consoleRenderer;
        this.page = Math.max(1, page);
        this.perPage = perPage;
        this.commandPrefix = commandPrefix;
    }

    @Override
    public Component renderChat(RenderContext ctx) {
        List<Component> lines = renderChatLines(ctx);
        Component result = Component.empty();
        for (Component c : lines) {
            result = result.append(c).append(Component.newline());
        }
        return result;
    }

    public List<Component> renderChatLines(RenderContext ctx) {
        if (items.isEmpty()) {
            return List.of(MM.parse("<" + Theme.C_MUTED + ">No items to display</" + Theme.C_MUTED + ">"));
        }

        int totalPages = (int) Math.ceil((double) items.size() / perPage);
        int actualPage = Math.min(page, totalPages);
        int start = (actualPage - 1) * perPage;
        int end = Math.min(start + perPage, items.size());

        var builder = MM.msg();

        for (int i = start; i < end; i++) {
            builder.line(chatRenderer.apply(items.get(i)));
        }

        if (totalPages > 1) {
            builder.space();
            Component nav = Component.empty();

            if (actualPage > 1) {
                nav = nav.append(MM
                        .parse("<" + Theme.C_ACCENT + "><bold>" + Theme.SYMBOL_ARROW_LEFT + "</bold></" + Theme.C_ACCENT
                                + ">")
                        .hoverEvent(HoverEvent.showText(MM.parse("Previous Page")))
                        .clickEvent(ClickEvent.runCommand(commandPrefix + " " + (actualPage - 1))));
            } else {
                nav = nav
                        .append(MM.parse("<" + Theme.C_SEP + ">" + Theme.SYMBOL_ARROW_LEFT + "</" + Theme.C_SEP + ">"));
            }

            nav = nav.append(MM.parse(
                    " <" + Theme.C_MUTED + ">Page " + actualPage + "/" + totalPages + "</" + Theme.C_MUTED + "> "));

            if (actualPage < totalPages) {
                nav = nav.append(MM
                        .parse("<" + Theme.C_ACCENT + "><bold>" + Theme.SYMBOL_ARROW_RIGHT + "</bold></"
                                + Theme.C_ACCENT + ">")
                        .hoverEvent(HoverEvent.showText(MM.parse("Next Page")))
                        .clickEvent(ClickEvent.runCommand(commandPrefix + " " + (actualPage + 1))));
            } else {
                nav = nav.append(
                        MM.parse("<" + Theme.C_SEP + ">" + Theme.SYMBOL_ARROW_RIGHT + "</" + Theme.C_SEP + ">"));
            }

            builder.line(nav);
        }

        return builder.getLines();
    }

    @Override
    public String renderConsole(RenderContext ctx) {
        if (items.isEmpty())
            return "No items.";

        StringBuilder sb = new StringBuilder();
        int totalPages = (int) Math.ceil((double) items.size() / perPage);
        int actualPage = Math.min(page, totalPages);
        int start = (actualPage - 1) * perPage;
        int end = Math.min(start + perPage, items.size());

        for (int i = start; i < end; i++) {
            sb.append(consoleRenderer.apply(items.get(i))).append("\n");
        }

        if (totalPages > 1) {
            sb.append("\n").append(Theme.ANSI_MUTED).append("Page ").append(actualPage).append("/").append(totalPages)
                    .append(Theme.ANSI_RESET);
        }

        return sb.toString();
    }
}
