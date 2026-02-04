package dev.objz.commandbridge.velocity.ui.cli;

public final class BoxDrawing {

    // Double-line box drawing
    public static final String DOUBLE_TOP_LEFT = "╔";
    public static final String DOUBLE_TOP_RIGHT = "╗";
    public static final String DOUBLE_BOTTOM_LEFT = "╚";
    public static final String DOUBLE_BOTTOM_RIGHT = "╝";
    public static final String DOUBLE_HORIZONTAL = "═";
    public static final String DOUBLE_VERTICAL = "║";
    public static final String DOUBLE_T_DOWN = "╦";
    public static final String DOUBLE_T_UP = "╩";
    public static final String DOUBLE_T_RIGHT = "╠";
    public static final String DOUBLE_T_LEFT = "╣";
    public static final String DOUBLE_CROSS = "╬";

    // Single-line box drawing
    public static final String SINGLE_TOP_LEFT = "┌";
    public static final String SINGLE_TOP_RIGHT = "┐";
    public static final String SINGLE_BOTTOM_LEFT = "└";
    public static final String SINGLE_BOTTOM_RIGHT = "┘";
    public static final String SINGLE_HORIZONTAL = "─";
    public static final String SINGLE_VERTICAL = "│";
    public static final String SINGLE_T_DOWN = "┬";
    public static final String SINGLE_T_UP = "┴";
    public static final String SINGLE_T_RIGHT = "├";
    public static final String SINGLE_T_LEFT = "┤";
    public static final String SINGLE_CROSS = "┼";

    // Block elements
    public static final String BLOCK_FULL = "█";
    public static final String BLOCK_LIGHT = "░";
    public static final String BLOCK_MEDIUM = "▒";
    public static final String BLOCK_DARK = "▓";

    private BoxDrawing() {
    }

    public static String line(int width, boolean doubleLine) {
        return (doubleLine ? DOUBLE_HORIZONTAL : SINGLE_HORIZONTAL).repeat(Math.max(0, width));
    }

    public static String boxTop(int width, boolean doubleLine) {
        if (doubleLine) {
            return DOUBLE_TOP_LEFT + DOUBLE_HORIZONTAL.repeat(width - 2) + DOUBLE_TOP_RIGHT;
        } else {
            return SINGLE_TOP_LEFT + SINGLE_HORIZONTAL.repeat(width - 2) + SINGLE_TOP_RIGHT;
        }
    }

    public static String boxBottom(int width, boolean doubleLine) {
        if (doubleLine) {
            return DOUBLE_BOTTOM_LEFT + DOUBLE_HORIZONTAL.repeat(width - 2) + DOUBLE_BOTTOM_RIGHT;
        } else {
            return SINGLE_BOTTOM_LEFT + SINGLE_HORIZONTAL.repeat(width - 2) + SINGLE_BOTTOM_RIGHT;
        }
    }

    public static String boxLine(String content, int width, boolean doubleLine) {
        String border = doubleLine ? DOUBLE_VERTICAL : SINGLE_VERTICAL;
        int contentWidth = width - 4;
        String padded = String.format(" %-" + contentWidth + "s ", content);
        return border + padded + border;
    }
}
