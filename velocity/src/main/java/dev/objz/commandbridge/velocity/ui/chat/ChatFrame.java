package dev.objz.commandbridge.velocity.ui.chat;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;

public final class ChatFrame {
    private final String title;
    private final List<Component> lines = new ArrayList<>();
    private Component hint;
    private int width;

    public ChatFrame(String title) {
        this.title = title;
    }

    public ChatFrame width(int width) {
        this.width = Math.max(0, width);
        return this;
    }

    public ChatFrame hint(Component hint) {
        this.hint = hint;
        return this;
    }

    public ChatFrame line(Component line) {
        lines.add(line);
        return this;
    }

    public ChatFrame lines(List<Component> moreLines) {
        lines.addAll(moreLines);
        return this;
    }

    public ChatFrame space() {
        lines.add(Component.empty());
        return this;
    }

    public List<Component> render() {
        int resolvedWidth = width > 0 ? width : computeWidth();
        resolvedWidth = Math.max(ChatLayout.DEFAULT_WIDTH_PX, resolvedWidth);
        List<Component> out = new ArrayList<>();
        out.add(Component.empty());
        out.add(ChatLayout.headerTitle(title, resolvedWidth));
        out.add(ChatLayout.headerUnderline(resolvedWidth));
        if (hint != null) {
            out.add(ChatLayout.center(hint, resolvedWidth));
        }
        out.addAll(lines);
        return out;
    }

    public void send(Audience audience) {
        if (audience == null) {
            return;
        }
        for (Component line : render()) {
            audience.sendMessage(line);
        }
    }


    private int computeWidth() {
        int max = ChatLayout.titleWidth(title);
        if (hint != null) {
            max = Math.max(max, ChatLayout.visibleLength(hint));
        }
        for (Component line : lines) {
            max = Math.max(max, ChatLayout.visibleLength(line));
        }
        return max;
    }
}
