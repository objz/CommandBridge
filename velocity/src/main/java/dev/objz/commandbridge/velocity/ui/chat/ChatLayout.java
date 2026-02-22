package dev.objz.commandbridge.velocity.ui.chat;

import dev.objz.commandbridge.util.MM;
import dev.objz.commandbridge.velocity.ui.Theme;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public final class ChatLayout {
    public static final int DEFAULT_WIDTH_PX = 200;
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    private ChatLayout() {
    }

    public static int visibleLength(Component component) {
        return pixelWidth(component);
    }

    public static int titleWidth(String title) {
        String safe = title == null ? "" : title.toUpperCase();
        return pixelWidth(safe, true);
    }

    public static Component headerTitle(String title, int width) {
        String safe = title == null ? "" : title.toUpperCase();
        Component titleComp = MM.parse("<gradient:" + Theme.C_PRIMARY + ":" + Theme.C_ACCENT + "><bold>" + safe + "</bold></gradient>");
        return center(titleComp, width);
    }

    public static Component headerUnderline(int width) {
        String line = repeatToWidth("━", Math.max(0, width));
        return MM.parse("<" + Theme.C_SEP + ">" + line + "</" + Theme.C_SEP + ">");
    }

    public static Component center(Component content, int width) {
        int len = visibleLength(content);
        int totalPadding = Math.max(0, width - len);
        int leftPad = totalPadding / 2;
        int rightPad = totalPadding - leftPad;
        return Component.empty()
                .append(Component.text(spacesForPixels(leftPad)))
                .append(content)
                .append(Component.text(spacesForPixels(rightPad)));
    }

    public static Component separator(int width) {
        String line = repeatToWidth("━", Math.max(0, width));
        return MM.parse("<" + Theme.C_SEP + ">" + line + "</" + Theme.C_SEP + ">");
    }

    public static String repeat(String value, int count) {
        if (count <= 0) {
            return "";
        }
        return value.repeat(count);
    }

    public static int pixelWidth(Component component) {
        return pixelWidth(component, false);
    }

    public static int pixelWidth(String text, boolean bold) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        int max = 0;
        int line = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n') {
                max = Math.max(max, line > 0 ? line - 1 : 0);
                line = 0;
                continue;
            }
            line += advanceWidth(c, bold);
        }
        max = Math.max(max, line > 0 ? line - 1 : 0);
        return max;
    }

    public static int spacesWidth(int spaces) {
        if (spaces <= 0) {
            return 0;
        }
        return spaces * spaceAdvance();
    }

    public static int spaceAdvance() {
        return advanceWidth(' ', false);
    }

    public static String spacesForPixels(int px) {
        if (px <= 0) {
            return "";
        }
        int advance = spaceAdvance();
        int count = Math.max(0, px / advance);
        return " ".repeat(count);
    }

    public static String repeatToWidth(String glyph, int targetPx) {
        if (glyph == null || glyph.isEmpty() || targetPx <= 0) {
            return "";
        }
        char c = glyph.charAt(0);
        int advance = advanceWidth(c, false);
        int count = Math.max(1, (targetPx + 1) / advance);
        String line = glyph.repeat(count);
        int width = pixelWidth(line, false);
        while (width > targetPx && count > 1) {
            count--;
            line = glyph.repeat(count);
            width = pixelWidth(line, false);
        }
        while (width < targetPx) {
            int nextWidth = pixelWidth(glyph.repeat(count + 1), false);
            if (nextWidth > targetPx) {
                break;
            }
            count++;
            width = nextWidth;
        }
        return line;
    }

    private static int pixelWidth(Component component, boolean parentBold) {
        return measureComponent(component, parentBold).width;
    }

    private static Measure measureComponent(Component component, boolean parentBold) {
        if (component == null) {
            return new Measure(0, false);
        }
        TextDecoration.State boldState = component.decoration(TextDecoration.BOLD);
        boolean bold = parentBold;
        if (boldState == TextDecoration.State.TRUE) {
            bold = true;
        } else if (boldState == TextDecoration.State.FALSE) {
            bold = false;
        }

        if (!(component instanceof TextComponent textComponent)) {
            int width = pixelWidth(PLAIN.serialize(component), bold);
            return new Measure(width, width > 0);
        }

        Measure total = appendMeasure(new Measure(0, false), pixelWidth(textComponent.content(), bold));
        for (Component child : component.children()) {
            total = appendMeasure(total, measureComponent(child, bold));
        }
        return total;
    }

    private static Measure appendMeasure(Measure base, int width) {
        if (width <= 0) {
            return base;
        }
        int combined = base.width + width + (base.hasContent ? 1 : 0);
        return new Measure(combined, true);
    }

    private static Measure appendMeasure(Measure base, Measure next) {
        if (!next.hasContent) {
            return base;
        }
        int combined = base.width + next.width + (base.hasContent ? 1 : 0);
        return new Measure(combined, true);
    }

    private record Measure(int width, boolean hasContent) {
    }

    private static int advanceWidth(char c, boolean bold) {
        int base = baseWidth(c);
        if (bold && c != ' ') {
            base += 1;
        }
        return base + 1;
    }

    private static int baseWidth(char c) {
        return switch (c) {
            case ' ' -> 3;
            case '!', '.', ',', ':', ';', '|', '¦', '¡' -> 1;
            case '\'' -> 1;
            case '"' -> 3;
            case '#', '$', '%', '&', '*', '+', '-', '/', '=', '^', '_', '?', '\\' -> 5;
            case 'i', 'l' -> 1;
            case 'I' -> 3;
            case 't', 'f', 'k' -> 4;
            case '(', ')', '<', '>', '{', '}' -> 4;
            case '[', ']' -> 3;
            case '`' -> 2;
            case '@' -> 6;
            case '\u2022', '\u2192', '\u2190', '\u2714', '\u2716', '\u2501', '\u2500', '\u2014', '\u2013' -> 5;
            case '\u2502' -> 1;
            default -> {
                if (Character.isLetterOrDigit(c)) {
                    yield 5;
                }
                yield 4;
            }
        };
    }
}
