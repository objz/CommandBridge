package dev.objz.commandbridge.util;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.ArrayList;
import java.util.List;

public final class MM {

    private static final String C_PRIMARY = "#7AA2FF";
    private static final String C_ACCENT = "#80E9FF";
    private static final String C_MUTED = "#94A3B8";
    private static final String C_SEP = "#3B4453";

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private MM() {
    }

    public static Component parse(String mm) {
        try {
            return MINI.deserialize(mm == null ? "" : mm);
        } catch (Exception e) {
            return Component.text(mm == null ? "" : mm);
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    public static Component title(String text) {
        return parse("<gradient:" + C_ACCENT + ":" + C_PRIMARY + "><bold>" + safe(text) + "</bold></gradient>");
    }

    public static Component header(String text) {
        return parse("<" + C_PRIMARY + "><bold>" + safe(text) + "</bold></" + C_PRIMARY + ">");
    }

    public static Component desc(String text) {
        return parse("<" + C_MUTED + ">" + safe(text) + "</" + C_MUTED + ">");
    }

    public static Component ok(String text) {
        return parse("<green>" + safe(text) + "</green>");
    }

    public static Component warn(String text) {
        return parse("<yellow>" + safe(text) + "</yellow>");
    }

    public static Component error(String text) {
        return parse("<red>" + safe(text) + "</red>");
    }

    public static Component muted(String text) {
        return parse("<" + C_MUTED + ">" + safe(text) + "</" + C_MUTED + ">");
    }

    public static Component accent(String text) {
        return parse("<" + C_ACCENT + ">" + safe(text) + "</" + C_ACCENT + ">");
    }

    public static Component sep() {
        return parse("<" + C_SEP + "> | </" + C_SEP + ">");
    }

    public static Component bullet(String mmContent) {
        return parse("<gradient:" + C_ACCENT + ":" + C_PRIMARY + ">• </gradient>" + safe(mmContent));
    }

    public static Component kv(String key, String value) {
        return parse("<" + C_MUTED + ">" + safe(key) + "</" + C_MUTED + "><" + C_SEP + ">:</" + C_SEP
                + "> <white>" + safe(value) + "</white>");
    }

    public static Component cmd(String command) {
        String label = "<gradient:" + C_ACCENT + ":" + C_PRIMARY + "><bold>" + safe(command)
                + "</bold></gradient>";
        Component base = parse(label);
        String hover = "run " + safe(command);
        return base.hoverEvent(HoverEvent.showText(parse("<" + C_MUTED + ">" + hover + "</" + C_MUTED + ">")))
                .clickEvent(ClickEvent.suggestCommand(safe(command)));
    }

    public static MessageBuilder msg() {
        return new MessageBuilder();
    }

    public static final class MessageBuilder {
        private final List<Component> lines = new ArrayList<>();

        public MessageBuilder superTitle(String text) {
            lines.add(MM.title(text));
            return this;
        }

        public MessageBuilder header(String text) {
            lines.add(MM.header(text));
            lines.add(MM.sep());
            return this;
        }

        public MessageBuilder line(Component c) {
            lines.add(c);
            return this;
        }

        public MessageBuilder line(String mm) {
            lines.add(MM.parse(mm));
            return this;
        }

        public MessageBuilder kv(String key, String val) {
            lines.add(MM.kv(key, val));
            return this;
        }

        public MessageBuilder cmdLine(String command, String description) {
            lines.add(MM.cmd(command).append(MM.sep()).append(MM.desc(description)));
            return this;
        }

        public MessageBuilder item(String mmContent) {
            lines.add(MM.bullet(mmContent));
            return this;
        }

        public MessageBuilder space() {
            lines.add(Component.empty());
            return this;
        }

        public List<Component> getLines() {
            return List.copyOf(lines);
        }

        public void send(Audience audience) {
            if (audience == null)
                return;
            for (Component line : lines) {
                audience.sendMessage(line);
            }
        }
    }
}
