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

    private final List<Script> loaded = new ArrayList<>();
    private final List<Script> enabled = new ArrayList<>();
    private final List<Script> disabled = new ArrayList<>();
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
        loaded.clear();
        enabled.clear();
        errors = 0;

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
                loaded.add(res.value);
                if (res.ok() && res.value != null) {
                    if (res.value.enabled())
                        enabled.add(res.value);
                    else
                        disabled.add(res.value);
                } else {
                    String header = "Script '" + p.getFileName() + "' invalid:";
                    Log.error(res.problems.toBulletedList(header));
                    disabled.add(res.value);
                    errors = res.problems.count();
                }
            } catch (Exception e) {
                Log.error(e, "Script '{}' invalid: unexpected error: {}", p.getFileName(), e.getMessage());
            }
        }

        if (logSummary) {
            Summary.scriptsSummary(loaded.size(), enabled.size(), disabled.size(), errors);
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
