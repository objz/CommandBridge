package dev.objz.commandbridge.scripting.migration;

import dev.objz.commandbridge.logging.Log;

import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;

/**
 * Loads version-step files from {@code migrations/} resources (e.g.
 * {@code 2-3.yml}),
 * merges chains of rules into a single pass, and applies the merged rules
 * directly
 * on raw YAML text lines to preserve comments, formatting, and layout.
 */
public final class ScriptMigrator {

    private static final Pattern VERSION_LINE = Pattern.compile("(version:\\s*)\\d+");
    private static final Pattern WILDCARD_PATTERN = Pattern.compile("^(.+?)\\[\\*]$");

    private final Load yamlLoader;
    private final TreeMap<Integer, MigrationStep> steps = new TreeMap<>();
    private final int currentVersion;

    public ScriptMigrator(int currentVersion) {
        this.currentVersion = currentVersion;
        this.yamlLoader = new Load(LoadSettings.builder().build());
        loadSteps();
    }

    /**
     * Returns the current (target) script version.
     */
    public int currentVersion() {
        return currentVersion;
    }

    /**
     * Detects the version of a raw YAML script string.
     *
     * @return the version number, or -1 if not parseable
     */
    public int detectVersion(String yaml) {
        try {
            Object root = yamlLoader.loadFromString(yaml);
            if (root instanceof Map<?, ?> map) {
                Object ver = map.get("version");
                if (ver instanceof Number n) {
                    return n.intValue();
                }
            }
        } catch (Exception ignored) {
        }
        return -1;
    }

    /**
     * Migrates a raw YAML script string from its detected version to the current
     * version.
     *
     * @return the result containing the migrated YAML or an error
     */
    public MigrationResult migrate(String yaml) {
        int from = detectVersion(yaml);
        if (from < 0) {
            return MigrationResult.error("Could not detect script version");
        }
        if (from == currentVersion) {
            return MigrationResult.skip(from);
        }
        if (from > currentVersion) {
            return MigrationResult.error(String.format(
                    "Script version %d is newer than the current version %d", from, currentVersion));
        }
        return migrate(yaml, from, currentVersion);
    }

    /**
     * Migrates a raw YAML script string from {@code from} to {@code to} by applying
     * merged rules directly on text lines, preserving all comments, blank lines,
     * and formatting.
     */
    public MigrationResult migrate(String yaml, int from, int to) {
        List<MigrationRule> merged = mergeRules(from, to);
        List<String> lines = new ArrayList<>(Arrays.asList(yaml.split("\n", -1)));

        List<TargetAction> modifyActions = new ArrayList<>();
        for (MigrationRule rule : merged) {
            if (rule.type() == MigrationRule.Type.ADD)
                continue;
            List<PathSegment> segments = parsePath(rule.path());
            List<int[]> targets = findTargetLines(lines, segments, 0, 0, lines.size());
            for (int[] target : targets) {
                modifyActions.add(new TargetAction(rule, target[0], target[1]));
            }
        }

        modifyActions.sort((a, b) -> Integer.compare(b.start, a.start));

        for (TargetAction action : modifyActions) {
            switch (action.rule.type()) {
                case REMOVE -> lines.subList(action.start, action.end).clear();
                case RENAME -> {
                    if (action.rule.to() != null) {
                        String newKey = lastSegmentKey(action.rule.to());
                        String line = lines.get(action.start);
                        lines.set(action.start, replaceKeyOnLine(line, extractKey(line), newKey));
                    }
                }
                default -> {
                }
            }
        }

        List<InsertAction> insertActions = new ArrayList<>();
        for (MigrationRule rule : merged) {
            if (rule.type() != MigrationRule.Type.ADD)
                continue;
            List<PathSegment> segments = parsePath(rule.path());
            if (segments.isEmpty())
                continue;

            List<PathSegment> parentSegments = segments.subList(0, segments.size() - 1);
            String leafKey = segments.get(segments.size() - 1).key();
            String valuePart = (rule.value() != null && !rule.value().isEmpty())
                    ? ": " + rule.value()
                    : ":";

            List<int[]> parents = parentSegments.isEmpty()
                    ? List.of(new int[] { -1, lines.size() })
                    : findTargetLines(lines, parentSegments, 0, 0, lines.size());

            for (int[] parent : parents) {
                int childrenStart = parent[0] >= 0 ? parent[0] + 1 : 0;
                int childrenEnd = parent[1];

                int childIndent = detectChildKeyIndent(lines, childrenStart, childrenEnd);
                if (childIndent < 0) {
                    childIndent = (parent[0] >= 0 ? leadingSpaces(lines.get(parent[0])) : 0) + 2;
                }

                int insertLine = findInsertionPoint(lines, childrenStart, childrenEnd, rule.after());
                insertActions.add(new InsertAction(insertLine,
                        " ".repeat(childIndent) + leafKey + valuePart));
            }
        }

        insertActions.sort((a, b) -> Integer.compare(b.lineIndex, a.lineIndex));
        for (InsertAction insert : insertActions) {
            lines.add(insert.lineIndex, insert.content);
        }

        updateVersionLine(lines, to);
        return MigrationResult.success(from, to, String.join("\n", lines), merged.size());
    }

    /**
     * Recursively finds target line ranges for the given path segments within
     * the specified line range.
     *
     * @return list of {@code [start, end)} line index pairs
     */
    private List<int[]> findTargetLines(List<String> lines, List<PathSegment> segments,
            int segIdx, int rangeStart, int rangeEnd) {
        if (segIdx >= segments.size()) {
            return List.of();
        }

        PathSegment seg = segments.get(segIdx);
        boolean isLast = segIdx == segments.size() - 1;

        if (seg.wildcard()) {
            List<int[]> items = findListItems(lines, rangeStart, rangeEnd);
            List<int[]> results = new ArrayList<>();
            for (int[] item : items) {
                results.addAll(findTargetLines(lines, segments, segIdx + 1, item[0], item[1]));
            }
            return results;
        }

        int childKeyIndent = detectChildKeyIndent(lines, rangeStart, rangeEnd);
        if (childKeyIndent < 0) {
            return List.of();
        }

        for (int i = rangeStart; i < rangeEnd; i++) {
            String line = lines.get(i);
            if (isBlankOrComment(line))
                continue;

            int indent = keyIndent(line);
            if (indent < childKeyIndent)
                break;
            if (indent > childKeyIndent)
                continue;

            String key = extractKey(line);
            if (!seg.key().equals(key))
                continue;

            int blockEnd = findValueBlockEnd(lines, i, rangeEnd);
            if (isLast) {
                return List.of(new int[] { i, blockEnd });
            }
            return findTargetLines(lines, segments, segIdx + 1, i + 1, blockEnd);
        }

        return List.of();
    }

    /**
     * Finds list item ranges (lines starting with {@code - }) at a consistent
     * indent level within the given range.
     *
     * @return list of {@code [start, end)} line index pairs for each item
     */
    private List<int[]> findListItems(List<String> lines, int start, int end) {
        int dashIndent = -1;
        for (int i = start; i < end; i++) {
            String line = lines.get(i);
            if (isBlankOrComment(line))
                continue;
            String stripped = line.stripLeading();
            if (stripped.startsWith("- ")) {
                dashIndent = leadingSpaces(line);
                break;
            }
        }
        if (dashIndent < 0)
            return List.of();

        List<int[]> items = new ArrayList<>();
        int itemStart = -1;

        for (int i = start; i < end; i++) {
            String line = lines.get(i);
            if (isBlankOrComment(line))
                continue;

            String stripped = line.stripLeading();
            if (stripped.startsWith("- ") && leadingSpaces(line) == dashIndent) {
                if (itemStart >= 0) {
                    items.add(new int[] { itemStart, i });
                }
                itemStart = i;
            }
        }

        if (itemStart >= 0) {
            items.add(new int[] { itemStart, end });
        }

        return items;
    }

    private int findValueBlockEnd(List<String> lines, int lineIndex, int rangeEnd) {
        int baseIndent = leadingSpaces(lines.get(lineIndex));
        int end = lineIndex + 1;

        while (end < rangeEnd) {
            String line = lines.get(end);
            if (isBlankOrComment(line)) {
                end++;
                continue;
            }
            if (leadingSpaces(line) > baseIndent) {
                end++;
            } else {
                break;
            }
        }

        while (end > lineIndex + 1 && isBlankOrComment(lines.get(end - 1))) {
            end--;
        }

        return end;
    }

    /**
     * Detects the key indent level of the first content line in the given range.
     */
    private int detectChildKeyIndent(List<String> lines, int start, int end) {
        for (int i = start; i < end; i++) {
            String line = lines.get(i);
            if (isBlankOrComment(line))
                continue;
            if (extractKey(line) != null) {
                return keyIndent(line);
            }
        }
        return -1;
    }

    /**
     * Finds the line index at which to insert a new key within a parent's children
     * range.
     * If {@code afterKey} is specified and found, returns the line after its value
     * block.
     * Otherwise returns the end of the children range (appends as the last child).
     */
    private int findInsertionPoint(List<String> lines, int childrenStart, int childrenEnd,
            String afterKey) {
        if (afterKey != null) {
            int childIndent = detectChildKeyIndent(lines, childrenStart, childrenEnd);
            if (childIndent >= 0) {
                for (int i = childrenStart; i < childrenEnd; i++) {
                    String line = lines.get(i);
                    if (isBlankOrComment(line))
                        continue;
                    int indent = keyIndent(line);
                    if (indent < childIndent)
                        break;
                    if (indent > childIndent)
                        continue;
                    if (afterKey.equals(extractKey(line))) {
                        return findValueBlockEnd(lines, i, childrenEnd);
                    }
                }
            }
        }
        return childrenEnd;
    }

    /**
     * Returns the effective key indent for a YAML line. For list item lines
     * ({@code - key: value}), this accounts for the {@code "- "} prefix.
     */
    private static int keyIndent(String line) {
        int spaces = leadingSpaces(line);
        String stripped = line.substring(spaces);
        if (stripped.startsWith("- ")) {
            String afterDash = stripped.substring(2);
            if (afterDash.indexOf(':') > 0) {
                return spaces + 2;
            }
        }
        return spaces;
    }

    /**
     * Extracts the key name from a YAML line, stripping leading whitespace
     * and any {@code "- "} list prefix.
     *
     * @return the key, or {@code null} if the line has no key
     */
    private static String extractKey(String line) {
        String stripped = line.stripLeading();
        if (stripped.isEmpty() || stripped.startsWith("#"))
            return null;
        if (stripped.startsWith("- ")) {
            stripped = stripped.substring(2);
        }
        int colon = stripped.indexOf(':');
        if (colon <= 0)
            return null;
        return stripped.substring(0, colon);
    }

    private static int leadingSpaces(String line) {
        int count = 0;
        for (int i = 0; i < line.length(); i++) {
            if (line.charAt(i) == ' ')
                count++;
            else
                break;
        }
        return count;
    }

    private static boolean isBlankOrComment(String line) {
        String stripped = line.stripLeading();
        return stripped.isEmpty() || stripped.startsWith("#");
    }

    private static String replaceKeyOnLine(String line, String oldKey, String newKey) {
        if (oldKey == null || newKey == null)
            return line;

        int spaces = leadingSpaces(line);
        String stripped = line.substring(spaces);
        String prefix = line.substring(0, spaces);

        if (stripped.startsWith("- ")) {
            prefix += "- ";
            stripped = stripped.substring(2);
        }

        if (stripped.startsWith(oldKey + ":")) {
            return prefix + newKey + stripped.substring(oldKey.length());
        }

        return line;
    }

    /**
     * Updates the {@code version:} line to the new version number.
     */
    private static void updateVersionLine(List<String> lines, int newVersion) {
        for (int i = 0; i < lines.size(); i++) {
            Matcher m = VERSION_LINE.matcher(lines.get(i));
            if (m.find()) {
                lines.set(i, m.replaceFirst("$1" + newVersion));
                return;
            }
        }
    }

    private String lastSegmentKey(String path) {
        int dot = path.lastIndexOf('.');
        return dot >= 0 ? path.substring(dot + 1) : path;
    }

    private List<PathSegment> parsePath(String path) {
        List<PathSegment> result = new ArrayList<>();
        String[] parts = path.split("\\.");
        for (String part : parts) {
            Matcher m = WILDCARD_PATTERN.matcher(part);
            if (m.matches()) {
                result.add(new PathSegment(m.group(1), false));
                result.add(new PathSegment(null, true));
            } else {
                result.add(new PathSegment(part, false));
            }
        }
        return result;
    }

    private record PathSegment(String key, boolean wildcard) {
    }

    private record TargetAction(MigrationRule rule, int start, int end) {
    }

    private record InsertAction(int lineIndex, String content) {
    }

    List<MigrationRule> mergeRules(int from, int to) {
        Map<String, String> pathMap = new LinkedHashMap<>();
        Map<String, MigrationRule> addedRules = new LinkedHashMap<>();
        List<MigrationRule> effective = new ArrayList<>();

        for (int v = from; v < to; v++) {
            MigrationStep step = steps.get(v);
            if (step == null) {
                continue;
            }

            for (MigrationRule rule : step.rules()) {
                switch (rule.type()) {
                    case REMOVE -> {
                        String resolvedPath = resolveCurrentPath(pathMap, rule.path());

                        if (addedRules.containsKey(resolvedPath)) {
                            String addPath = resolvedPath;
                            effective.removeIf(r -> r.type() == MigrationRule.Type.ADD
                                    && r.path().equals(addPath));
                            addedRules.remove(resolvedPath);
                            continue;
                        }

                        if (pathMap.containsKey(resolvedPath) && pathMap.get(resolvedPath) == null) {
                            continue;
                        }
                        String original = findOriginalPath(pathMap, resolvedPath);
                        if (original != null) {
                            effective.removeIf(r -> r.type() == MigrationRule.Type.RENAME
                                    && r.path().equals(original));
                            pathMap.put(original, null);
                            effective.add(new MigrationRule(MigrationRule.Type.REMOVE, original));
                        } else {
                            pathMap.put(resolvedPath, null);
                            effective.add(new MigrationRule(MigrationRule.Type.REMOVE, resolvedPath));
                        }
                    }
                    case RENAME -> {
                        if (rule.to() == null) {
                            continue;
                        }
                        String resolvedPath = resolveCurrentPath(pathMap, rule.path());

                        if (addedRules.containsKey(resolvedPath)) {
                            MigrationRule oldAdd = addedRules.remove(resolvedPath);
                            String addPath = resolvedPath;
                            effective.removeIf(r -> r.type() == MigrationRule.Type.ADD
                                    && r.path().equals(addPath));
                            MigrationRule newAdd = new MigrationRule(MigrationRule.Type.ADD,
                                    rule.to(), null, oldAdd.value(), oldAdd.after());
                            addedRules.put(rule.to(), newAdd);
                            effective.add(newAdd);
                            continue;
                        }

                        if (pathMap.containsKey(resolvedPath) && pathMap.get(resolvedPath) == null) {
                            continue;
                        }
                        String original = findOriginalPath(pathMap, resolvedPath);
                        if (original != null) {
                            effective.removeIf(r -> r.type() == MigrationRule.Type.RENAME
                                    && r.path().equals(original));
                            pathMap.put(original, rule.to());
                            effective.add(new MigrationRule(MigrationRule.Type.RENAME, original, rule.to()));
                        } else {
                            pathMap.put(resolvedPath, rule.to());
                            effective.add(new MigrationRule(MigrationRule.Type.RENAME, resolvedPath, rule.to()));
                        }
                    }
                    case ADD -> {
                        addedRules.put(rule.path(), rule);
                        effective.add(rule);
                    }
                }
            }
        }

        return List.copyOf(effective);
    }

    private String resolveCurrentPath(Map<String, String> pathMap, String path) {
        for (var entry : pathMap.entrySet()) {
            if (path.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        return path;
    }

    private String findOriginalPath(Map<String, String> pathMap, String currentPath) {
        for (var entry : pathMap.entrySet()) {
            if (currentPath.equals(entry.getValue())) {
                return entry.getKey();
            }
            if (currentPath.equals(entry.getKey()) && entry.getValue() != null) {
                return entry.getKey();
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private void loadSteps() {
        for (int from = 1; from < currentVersion; from++) {
            String filename = from + "-" + (from + 1) + ".yml";
            String resourcePath = "migrations/" + filename;

            try (InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
                if (in == null) {
                    continue;
                }

                Object root = yamlLoader.loadFromInputStream(in);
                if (!(root instanceof Map<?, ?> map)) {
                    Log.warn("Migration file '{}' is not a valid YAML mapping, skipping", filename);
                    continue;
                }

                Object rulesObj = map.get("rules");
                List<MigrationRule> rules = new ArrayList<>();

                if (rulesObj instanceof List<?> ruleList) {
                    for (Object entry : ruleList) {
                        if (!(entry instanceof Map<?, ?> ruleMap)) {
                            continue;
                        }

                        String typeStr = Objects.toString(ruleMap.get("type"), "").toUpperCase(Locale.ROOT);
                        String path = Objects.toString(ruleMap.get("path"), null);
                        String to = ruleMap.containsKey("to")
                                ? Objects.toString(ruleMap.get("to"), null)
                                : null;
                        String value = ruleMap.containsKey("value")
                                ? Objects.toString(ruleMap.get("value"), null)
                                : null;
                        String after = ruleMap.containsKey("after")
                                ? Objects.toString(ruleMap.get("after"), null)
                                : null;

                        if (path == null || path.isBlank()) {
                            continue;
                        }

                        MigrationRule.Type type;
                        try {
                            type = MigrationRule.Type.valueOf(typeStr);
                        } catch (IllegalArgumentException e) {
                            Log.warn("Unknown migration rule type '{}' in '{}', skipping",
                                    typeStr, filename);
                            continue;
                        }

                        rules.add(new MigrationRule(type, path, to, value, after));
                    }
                }

                MigrationStep step = new MigrationStep(from, from + 1, rules);
                steps.put(from, step);
                Log.debug("Loaded migration step {} -> {} with {} rule(s)",
                        from, from + 1, rules.size());

            } catch (Exception e) {
                Log.warn("Failed to load migration file '{}': {}", filename, e.getMessage());
            }
        }
    }
}
