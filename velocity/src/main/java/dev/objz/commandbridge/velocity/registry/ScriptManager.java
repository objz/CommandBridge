package dev.objz.commandbridge.velocity.registry;

import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.logging.StatusLog;
import dev.objz.commandbridge.scripting.DebugPrinter;
import dev.objz.commandbridge.scripting.ScriptLoader;
import dev.objz.commandbridge.scripting.ScriptLoader.LoadResult;
import dev.objz.commandbridge.scripting.model.Script;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public final class ScriptManager {
	private final Path scriptsDir;

	private final List<Script> all = new ArrayList<>();
	private final List<Script> enabled = new ArrayList<>();
	private long totalErrors;

	public ScriptManager(Path dataDir) {
		this.scriptsDir = dataDir.resolve("scripts");
	}

	public void loadAll() {
		all.clear();
		enabled.clear();
		totalErrors = 0;

		try {
			Files.createDirectories(scriptsDir);
		} catch (IOException e) {
			Log.error(e, "Cannot create scripts directory at '{}'", scriptsDir);
			return;
		}

		int loaded = 0, failed = 0;

		try (Stream<Path> files = Files.list(scriptsDir)) {
			for (Path p : (Iterable<Path>) files::iterator) {
				if (!isYaml(p))
					continue;

				try (var in = new FileInputStream(p.toFile())) {
					LoadResult<Script> res = ScriptLoader.loadResult(Script.class, in);
					if (res.ok() && res.value != null) {
						all.add(res.value);
						if (res.value.enabled())
							enabled.add(res.value);
						loaded++;
					} else {
						failed++;
						int cnt = res.problems.count();
						totalErrors += cnt;
						String header = "Script '" + p.getFileName() + "' invalid:";
						Log.error(res.problems.toBulletedList(header));
					}
				} catch (Exception e) {
					failed++;
					totalErrors++;
					String header = "Script '" + p.getFileName() + "' invalid:";
					Log.error(header + System.lineSeparator() +
							"  - script: unexpected error: " + e.getMessage());
				}
			}
		} catch (IOException e) {
			Log.error(e, "Failed to list scripts at '{}'", scriptsDir);
			return;
		}

		if (loaded + failed == 0) {
			Log.warn("No script files found (looking for *.yml or *.yaml)");
			return;
		}

		if (Log.isDebug() && !all.isEmpty()) {
			Log.debug("\n" + DebugPrinter.printGrid(all));
		}

		long disabled = (loaded - enabled.size()) + failed;

		StatusLog.scriptsSummary(loaded, enabled.size(), disabled, totalErrors);
	}

	public List<Script> all() {
		return List.copyOf(all);
	}

	public List<Script> enabled() {
		return List.copyOf(enabled);
	}

	public long totalErrors() {
		return totalErrors;
	}

	private static boolean isYaml(Path p) {
		if (!Files.isRegularFile(p))
			return false;
		String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
		return n.endsWith(".yml") || n.endsWith(".yaml");
	}
}
