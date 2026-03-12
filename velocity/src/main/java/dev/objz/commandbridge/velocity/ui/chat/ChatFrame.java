package dev.objz.commandbridge.velocity.ui.chat;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;

public final class ChatFrame {
    private final String title;
    private final List<Component> lines = new ArrayList<>();
    private Component hint;

    public ChatFrame(String title) {
        this.title = title;
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
        List<Component> out = new ArrayList<>();
        out.add(Component.empty());
        out.add(ChatLayout.headerTitle(title));
        out.add(ChatLayout.headerUnderline());
        if (hint != null) {
            out.add(hint);
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
}
