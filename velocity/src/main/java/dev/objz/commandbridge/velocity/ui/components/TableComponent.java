package dev.objz.commandbridge.velocity.ui.components;

import dev.objz.commandbridge.util.MM;
import dev.objz.commandbridge.velocity.ui.BoxDrawing;
import dev.objz.commandbridge.velocity.ui.RenderContext;
import dev.objz.commandbridge.velocity.ui.Theme;
import dev.objz.commandbridge.velocity.ui.UIComponent;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;

public class TableComponent implements UIComponent {

    private final String[] headers;
    private final List<String[]> rows;
    private final int consoleWidth;

    public TableComponent(String... headers) {
        this.headers = headers;
        this.rows = new ArrayList<>();
        this.consoleWidth = 80;
    }

    public void addRow(String... values) {
        if (values.length != headers.length) {
            throw new IllegalArgumentException("Row length mismatch");
        }
        rows.add(values);
    }

    @Override
    public Component renderChat(RenderContext ctx) {
        var builder = MM.msg();
        
        StringBuilder headerMm = new StringBuilder();
        for (int i = 0; i < headers.length; i++) {
            headerMm.append("<gradient:")
                   .append(Theme.C_PRIMARY)
                   .append(":")
                   .append(Theme.C_ACCENT)
                   .append("><bold>")
                   .append(headers[i])
                   .append("</bold></gradient>");
            if (i < headers.length - 1) {
                headerMm.append(" <").append(Theme.C_SEP).append(">│</").append(Theme.C_SEP).append("> ");
            }
        }
        builder.line(headerMm.toString());
        
        builder.line("<" + Theme.C_SEP + "><st>" + " ".repeat(50) + "</st></" + Theme.C_SEP + ">");

        for (int r = 0; r < rows.size(); r++) {
            String[] row = rows.get(r);
            StringBuilder rowMm = new StringBuilder();
            for (int i = 0; i < row.length; i++) {
                rowMm.append("<white>").append(row[i]).append("</white>");
                if (i < row.length - 1) {
                    rowMm.append(" <").append(Theme.C_SEP).append(">│</").append(Theme.C_SEP).append("> ");
                }
            }
            builder.line(rowMm.toString());
        }

        Component result = Component.empty();
        for (Component c : builder.getLines()) {
            result = result.append(c).append(Component.newline());
        }
        return result;
    }

    @Override
    public String renderConsole(RenderContext ctx) {
        // Calculate column widths
        int[] widths = new int[headers.length];
        for (int i = 0; i < headers.length; i++) {
            widths[i] = headers[i].length();
        }
        for (String[] row : rows) {
            for (int i = 0; i < row.length; i++) {
                widths[i] = Math.max(widths[i], stripAnsi(row[i]).length());
            }
        }

        for (int i = 0; i < widths.length; i++) {
            widths[i] += 2; 
        }

        int totalWidth = 1; 
        for (int w : widths) totalWidth += w + 1; 

        StringBuilder sb = new StringBuilder();
        
        // Top border
        sb.append(Theme.ANSI_PRIMARY).append(Theme.ANSI_BOLD);
        sb.append(BoxDrawing.DOUBLE_TOP_LEFT);
        for (int i = 0; i < headers.length; i++) {
            sb.append(BoxDrawing.DOUBLE_HORIZONTAL.repeat(widths[i]));
            if (i < headers.length - 1) {
                sb.append(BoxDrawing.DOUBLE_T_DOWN);
            }
        }
        sb.append(BoxDrawing.DOUBLE_TOP_RIGHT);
        sb.append(Theme.ANSI_RESET).append("\n");
        
        // Header row
        sb.append(Theme.ANSI_PRIMARY).append(Theme.ANSI_BOLD);
        sb.append(BoxDrawing.DOUBLE_VERTICAL);
        for (int i = 0; i < headers.length; i++) {
            sb.append(Theme.ANSI_ACCENT).append(Theme.ANSI_BOLD);
            sb.append(pad(headers[i], widths[i]));
            sb.append(Theme.ANSI_PRIMARY);
            sb.append(BoxDrawing.DOUBLE_VERTICAL);
        }
        sb.append(Theme.ANSI_RESET).append("\n");
        
        // Middle separator
        sb.append(Theme.ANSI_PRIMARY);
        sb.append(BoxDrawing.DOUBLE_T_RIGHT);
        for (int i = 0; i < headers.length; i++) {
            sb.append(BoxDrawing.DOUBLE_HORIZONTAL.repeat(widths[i]));
            if (i < headers.length - 1) {
                sb.append(BoxDrawing.DOUBLE_CROSS);
            }
        }
        sb.append(BoxDrawing.DOUBLE_T_LEFT);
        sb.append(Theme.ANSI_RESET).append("\n");

        // Data rows
        for (String[] row : rows) {
            sb.append(Theme.ANSI_PRIMARY);
            sb.append(BoxDrawing.DOUBLE_VERTICAL);
            for (int i = 0; i < row.length; i++) {
                sb.append(Theme.ANSI_RESET);
                sb.append(pad(row[i], widths[i]));
                sb.append(Theme.ANSI_PRIMARY);
                sb.append(BoxDrawing.DOUBLE_VERTICAL);
            }
            sb.append(Theme.ANSI_RESET).append("\n");
        }
        
        // Bottom border
        sb.append(Theme.ANSI_PRIMARY);
        sb.append(BoxDrawing.DOUBLE_BOTTOM_LEFT);
        for (int i = 0; i < headers.length; i++) {
            sb.append(BoxDrawing.DOUBLE_HORIZONTAL.repeat(widths[i]));
            if (i < headers.length - 1) {
                sb.append(BoxDrawing.DOUBLE_T_UP);
            }
        }
        sb.append(BoxDrawing.DOUBLE_BOTTOM_RIGHT);
        sb.append(Theme.ANSI_RESET).append("\n");

        return sb.toString();
    }

    private String stripAnsi(String s) {
        if (s == null) return "";
        return s.replaceAll("<[^>]*>", "").replaceAll("\u001B\\[[;\\d]*m", "");
    }

    private String pad(String s, int width) {
        String clean = stripAnsi(s);
        int len = clean.length();
        int leftPad = 1;
        int rightPad = width - len - leftPad;
        return " ".repeat(leftPad) + s + " ".repeat(Math.max(0, rightPad));
    }
}
