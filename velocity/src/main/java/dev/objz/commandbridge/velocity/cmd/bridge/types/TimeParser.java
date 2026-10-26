package dev.objz.commandbridge.velocity.cmd.bridge.types;

import java.util.Locale;

/** Reads {@code 20}, {@code 1s}, {@code 1d} or {@code 5t} as game ticks for {@link TimeArgument}. */
final class TimeParser {

    private TimeParser() {
    }

    static int ticks(String raw) {
        if (raw == null || raw.isBlank())
            throw new IllegalArgumentException("Time value cannot be empty");
        String input = raw.toLowerCase(Locale.ROOT).trim();
        if (!input.matches("\\d+[dst]?"))
            throw new IllegalArgumentException("Invalid time format");
        char last = input.charAt(input.length() - 1);
        boolean hasUnit = !Character.isDigit(last);
        String digits = hasUnit ? input.substring(0, input.length() - 1) : input;
        int factor = !hasUnit || last == 't' ? 1 : last == 's' ? 20 : 24000;
        try {
            // multiplyExact reports overflow, which would otherwise wrap to a negative tick count
            return Math.multiplyExact(Integer.parseInt(digits), factor);
        } catch (NumberFormatException | ArithmeticException e) {
            throw new IllegalArgumentException("Time value is too large: " + input);
        }
    }
}
