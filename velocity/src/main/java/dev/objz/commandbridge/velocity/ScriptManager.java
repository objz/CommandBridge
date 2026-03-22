package dev.objz.commandbridge.velocity;

import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.logging.Summary;
import dev.objz.commandbridge.scripting.ScriptLoader;
import dev.objz.commandbridge.scripting.ScriptLoader.LoadResult;
import dev.objz.commandbridge.scripting.model.Script;
import dev.objz.commandbridge.scripting.platform.PlatformFeatures;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public final class ScriptManager {
    private final Path scriptsDir;
    private final PlatformFeatures platformFeatures;

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

    private static boolean isYaml(Path p) {
        if (!Files.isRegularFile(p))
            return false;
        String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
        return n.endsWith(".yml") || n.endsWith(".yaml");
    }
}
