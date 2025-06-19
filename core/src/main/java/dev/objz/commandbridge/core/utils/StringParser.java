package dev.objz.commandbridge.core.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringParser {
    private final Map<String, String> placeholders = new HashMap<>();
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("%([^%]+)%");

    public void add(String placeholder, String value) {
        placeholders.put(placeholder, value);
    }

    public void remove(String placeholder) {
        placeholders.remove(placeholder);
    }

    public String parse(String command, String[] args) {
        if (!placeholders.isEmpty()) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                String placeholder = entry.getKey();
                String value = entry.getValue();
                command = command.replace(placeholder, value != null ? value : "");
            }
        }

        command = command.replace("%args%", args != null ? String.join(" ", args) : "");

        if (args != null) {
            command = replaceArgs(command, args);
        }

        return command;
    }

    public Result validate(String command, String[] args) {
        Set<String> unresolved = new HashSet<>();
        Set<String> found = findAll(command);

        for (String placeholder : found) {
            if (placeholder.equals("%args%")) {
                continue;
            }

            if (placeholder.matches("%arg\\[\\d+\\]%")) {
                Pattern argPattern = Pattern.compile("%arg\\[(\\d+)\\]%");
                Matcher matcher = argPattern.matcher(placeholder);
                if (matcher.find()) {
                    int index = Integer.parseInt(matcher.group(1));
                    if (args == null || index >= args.length) {
                        unresolved.add(placeholder);
                    }
                }
                continue;
            }

            if (!placeholders.containsKey(placeholder)) {
                unresolved.add(placeholder);
            }
        }

        String parsed = parse(command, args);
        return new Result(parsed, unresolved, unresolved.isEmpty());
    }

    private Set<String> findAll(String command) {
        Set<String> placeholders = new HashSet<>();
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(command);

        while (matcher.find()) {
            placeholders.add(matcher.group(0));
        }

        return placeholders;
    }

    public void clear() {
        placeholders.clear();
    }

    private String replaceArgs(String command, String[] args) {
        Pattern pattern = Pattern.compile("%arg\\[(\\d+)]%");
        Matcher matcher = pattern.matcher(command);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            int index = Integer.parseInt(matcher.group(1));
            String replacement = (index >= 0 && index < args.length) ? args[index] : "";
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(result);
        return result.toString();
    }

    public static StringParser create() {
        return new StringParser();
    }

    public static class Result {
        private final String parsed;
        private final Set<String> unresolved;
        private final boolean valid;

        public Result(String parsed, Set<String> unresolved, boolean valid) {
            this.parsed = parsed;
            this.unresolved = unresolved;
            this.valid = valid;
        }

        public String getParsed() {
            return parsed;
        }

        public Set<String> getUnresolved() {
            return unresolved;
        }

        public boolean isValid() {
            return valid;
        }
    }
}
