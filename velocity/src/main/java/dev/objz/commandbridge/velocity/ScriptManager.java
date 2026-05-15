package dev.objz.commandbridge.velocity;

import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.logging.Summary;
import dev.objz.commandbridge.scripting.ScriptLoader;
import dev.objz.commandbridge.scripting.ScriptLoader.LoadResult;
import dev.objz.commandbridge.scripting.model.Script;
import dev.objz.commandbridge.scripting.platform.PlatformFeatures;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class ScriptManager {

    private static final Pattern ENABLED_LINE = Pattern.compile(
            "(?m)^enabled:[ \\t]*(true|false)([ \\t]*(#.*)?)$");
    private static final Pattern VERSION_LINE = Pattern.compile("(?m)^version:[ \\t]*.*$");

    private final Path scriptsDir;
    private final PlatformFeatures platformFeatures;

    private final Map<String, Path> filesByName = new HashMap<>();

    private volatile List<Script> loaded = List.of();
    private volatile List<Script> enabled = List.of();
    private volatile List<Script> disabled = List.of();
    private long errors;

    public ScriptManager(Path dataDir, PlatformFeatures platformFeatures) {
        this.scriptsDir = dataDir.resolve("scripts");
        this.platformFeatures = platformFeatures != null
                ? platformFeatures
                : PlatformFeatures.none();
    }

    public void loadAll() {
        loadAll(true);
    }

    public void loadAll(boolean logSummary) {
        List<Script> newLoaded = new ArrayList<>();
        List<Script> newEnabled = new ArrayList<>();
        List<Script> newDisabled = new ArrayList<>();
        Map<String, Path> newFiles = new HashMap<>();
        long newErrors = 0;

        try {
            Files.createDirectories(scriptsDir);
        } catch (IOException e) {
            Log.error(e, "Cannot create scripts directory at '{}'", scriptsDir);
            return;
        }

        List<Path> yamlFiles;
        try (Stream<Path> files = Files.list(scriptsDir)) {
            yamlFiles = files.filter(ScriptManager::isYaml).toList();
        } catch (IOException e) {
            Log.error(e, "Failed to list scripts at '{}'", scriptsDir);
            return;
        }

        for (Path p : yamlFiles) {
            try (var in = Files.newInputStream(p)) {
                LoadResult<Script> res = ScriptLoader.loadResult(Script.class, in,
                        platformFeatures);
                newLoaded.add(res.value);
                if (res.ok() && res.value != null) {
                    Log.debug("Loaded script '{}' from '{}' (enabled={}, commands={})",
                            res.value.name(), p.getFileName(), res.value.enabled(),
                            res.value.commands() != null ? res.value.commands().size() : 0);
                    if (res.value.name() != null) {
                        newFiles.put(res.value.name(), p);
                    }
                    if (res.value.enabled())
                        newEnabled.add(res.value);
                    else
                        newDisabled.add(res.value);
                } else {
                    String header = "Script '" + p.getFileName() + "' invalid:";
                    Log.error(res.problems.toBulletedList(header));
                    newDisabled.add(res.value);
                    newErrors += res.problems.count();
                }
            } catch (Exception e) {
                Log.error(e, "Script '{}' invalid: unexpected error: {}", p.getFileName(), e.getMessage());
            }
        }

        // Atomic swap — readers see a consistent snapshot
        this.loaded = List.copyOf(newLoaded);
        this.enabled = List.copyOf(newEnabled);
        this.disabled = List.copyOf(newDisabled);
        synchronized (filesByName) {
            filesByName.clear();
            filesByName.putAll(newFiles);
        }
        this.errors = newErrors;

        if (logSummary) {
            Summary.scriptsSummary(newLoaded.size(), newEnabled.size(), newDisabled.size(), newErrors);
        }
    }

    public List<Script> loaded() {
        return loaded;
    }

    public List<Script> disabled() {
        return disabled;
    }

    public List<Script> enabled() {
        return enabled;
    }

    public long errors() {
        return errors;
    }

    public Path scriptsDir() {
        return scriptsDir;
    }

    public Optional<Script> findByName(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        for (Script s : loaded) {
            if (s != null && name.equalsIgnoreCase(s.name())) {
                return Optional.of(s);
            }
        }
        return Optional.empty();
    }

    public Optional<Path> filePathOf(String name) {
        if (name == null) {
            return Optional.empty();
        }
        synchronized (filesByName) {
            return Optional.ofNullable(filesByName.get(name));
        }
    }

    public ToggleResult setEnabled(String name, boolean target) {
        Optional<Path> pathOpt = filePathOf(name);
        if (pathOpt.isEmpty()) {
            return ToggleResult.notFound();
        }
        Path file = pathOpt.get();

        String original;
        try {
            original = Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            Log.error(e, "Failed to read script file '{}': {}", file, e.getMessage());
            return ToggleResult.ioError(e.getMessage());
        }

        String desired = String.valueOf(target);
        Matcher m = ENABLED_LINE.matcher(original);
        String updated;
        if (m.find()) {
            if (desired.equals(m.group(1))) {
                return ToggleResult.unchanged();
            }
            String trailing = m.group(2) != null ? m.group(2) : "";
            String replacement = "enabled: " + desired + trailing;
            updated = original.substring(0, m.start())
                    + replacement
                    + original.substring(m.end());
        } else {
            Matcher v = VERSION_LINE.matcher(original);
            String insertion = "enabled: " + desired + System.lineSeparator();
            if (v.find()) {
                int insertAt = v.end();
                String after = original.substring(insertAt);
                String prefix = after.startsWith("\n") || after.startsWith("\r") ? "" : System.lineSeparator();
                updated = original.substring(0, insertAt) + prefix + insertion + after;
            } else {
                updated = insertion + original;
            }
        }

        try {
            Files.writeString(file, updated, StandardCharsets.UTF_8);
        } catch (IOException e) {
            Log.error(e, "Failed to write script file '{}': {}", file, e.getMessage());
            return ToggleResult.ioError(e.getMessage());
        }

        loadAll(false);
        return ToggleResult.changed();
    }

    public enum ToggleStatus {
        CHANGED, UNCHANGED, NOT_FOUND, IO_ERROR
    }

    public record ToggleResult(ToggleStatus status, String error) {
        public static ToggleResult changed() {
            return new ToggleResult(ToggleStatus.CHANGED, null);
        }

        public static ToggleResult unchanged() {
            return new ToggleResult(ToggleStatus.UNCHANGED, null);
        }

        public static ToggleResult notFound() {
            return new ToggleResult(ToggleStatus.NOT_FOUND, null);
        }

        public static ToggleResult ioError(String message) {
            return new ToggleResult(ToggleStatus.IO_ERROR, message);
        }
    }

    private static boolean isYaml(Path p) {
        if (!Files.isRegularFile(p))
            return false;
        String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
        return n.endsWith(".yml") || n.endsWith(".yaml");
    }
}
