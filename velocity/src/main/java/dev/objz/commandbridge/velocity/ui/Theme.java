package dev.objz.commandbridge.velocity.ui;

import dev.objz.commandbridge.logging.Log;

public final class Theme {
    
    // Mini Message - Chat
    public static final String C_PRIMARY = "#7AA2FF";
    public static final String C_ACCENT = "#80E9FF";
    public static final String C_SUCCESS = "#4ADE80";
    public static final String C_WARN = "#FACC15";
    public static final String C_ERROR = "#F87171";
    public static final String C_MUTED = "#94A3B8";
    public static final String C_DARK = "#1E293B";
    public static final String C_SEP = "#3B4453";

    // ANSI - Console
    public static final String ANSI_RESET = Log.RESET;
    public static final String ANSI_PRIMARY = Log.CYAN; 
    public static final String ANSI_ACCENT = Log.CYAN;
    public static final String ANSI_SUCCESS = Log.GREEN;
    public static final String ANSI_WARN = Log.YELLOW;
    public static final String ANSI_ERROR = Log.RED;
    public static final String ANSI_MUTED = Log.GRAY;
    public static final String ANSI_SEP = "\u001B[90m";
    public static final String ANSI_BOLD = Log.BOLD;

    public static final String SYMBOL_ARROW_RIGHT = "→";
    public static final String SYMBOL_ARROW_LEFT = "←";
    public static final String SYMBOL_BULLET = "•";
    public static final String SYMBOL_CHECK = "✔";
    public static final String SYMBOL_CROSS = "✖";
    
    public static final String ANSI_SYMBOL_ARROW_RIGHT = "->";
    public static final String ANSI_SYMBOL_ARROW_LEFT = "<-";
    public static final String ANSI_SYMBOL_BULLET = "*";

    private Theme() {}
}
