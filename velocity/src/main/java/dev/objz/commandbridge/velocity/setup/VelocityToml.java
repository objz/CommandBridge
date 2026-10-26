package dev.objz.commandbridge.velocity.setup;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Adds servers to velocity.toml by inserting lines, so its comments and layout stay as they are. */
public final class VelocityToml {

    private static final Pattern TABLE = Pattern.compile("^\\s*\\[");
    private static final Pattern KEY = Pattern.compile("^\\s*(?:\"([^\"]*)\"|([A-Za-z0-9_-]+))\\s*=");
    private static final Pattern BARE = Pattern.compile("[A-Za-z0-9_-]+");

    private VelocityToml() {
    }

    /** {@code servers} maps a name to {@code host:port}; names already in [servers] are left alone. */
    public static String addServers(String toml, Map<String, String> servers) {
        String nl = toml.contains("\r\n") ? "\r\n" : "\n";
        List<String> lines = new ArrayList<>(List.of(toml.split("\r?\n", -1)));

        int start = -1;
        int end = lines.size();
        for (int i = 0; i < lines.size(); i++) {
            if (!TABLE.matcher(lines.get(i)).find())
                continue;
            if (start >= 0) {
                end = i;
                break;
            }
            if (lines.get(i).trim().equals("[servers]"))
                start = i;
        }

        Set<String> existing = new HashSet<>();
        int tryLine = -1;
        int lastContent = start;
        if (start >= 0) {
            for (int i = start + 1; i < end; i++) {
                Matcher m = KEY.matcher(lines.get(i));
                if (m.find()) {
                    String key = m.group(1) != null ? m.group(1) : m.group(2);
                    if (key.equals("try") && tryLine < 0)
                        tryLine = i;
                    else
                        existing.add(key);
                }
                if (!lines.get(i).isBlank() && tryLine < 0)
                    lastContent = i;
            }
        }

        List<String> added = new ArrayList<>();
        servers.forEach((name, address) -> {
            if (!existing.contains(name))
                added.add(entry(name, address));
        });
        if (added.isEmpty())
            return toml;

        if (start < 0) {
            StringBuilder sb = new StringBuilder(toml);
            if (!toml.isEmpty() && !toml.endsWith("\n"))
                sb.append(nl);
            sb.append(nl).append("[servers]").append(nl);
            added.forEach(line -> sb.append(line).append(nl));
            return sb.toString();
        }
        lines.addAll(tryLine >= 0 ? tryLine : lastContent + 1, added);
        return String.join(nl, lines);
    }

    public static void addServers(Path file, Map<String, String> servers) throws IOException {
        String before = Files.readString(file, StandardCharsets.UTF_8);
        String after = addServers(before, servers);
        if (after.equals(before))
            return;
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(tmp, after, StandardCharsets.UTF_8);
        Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    private static String entry(String name, String address) {
        String key = BARE.matcher(name).matches() ? name : "\"" + escape(name) + "\"";
        return key + " = \"" + escape(address) + "\"";
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
