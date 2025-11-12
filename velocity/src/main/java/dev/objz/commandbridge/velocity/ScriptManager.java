package dev.objz.commandbridge.velocity;

import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.logging.Summary;
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

	private final List<Script> loaded = new ArrayList<>();
	private final List<Script> enabled = new ArrayList<>();
	private final List<Script> disabled = new ArrayList<>();
	private long errors;

	public ScriptManager(Path dataDir) {
		this.scriptsDir = dataDir.resolve("scripts");
	}

	public void loadAll() {
		loaded.clear();
		enabled.clear();
		errors = 0;

		try {
			Files.createDirectories(scriptsDir);
		} catch (IOException e) {
			Log.error(e, "Cannot create scripts directory at '{}'", scriptsDir);
			return;
		}

		try (Stream<Path> files = Files.list(scriptsDir)) {
			for (Path p : (Iterable<Path>) files::iterator) {
				if (!isYaml(p))
					continue;

				try (var in = new FileInputStream(p.toFile())) {
					LoadResult<Script> res = ScriptLoader.loadResult(Script.class, in);
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
					String header = "Script '" + p.getFileName() + "' invalid:";
					Log.error(header + System.lineSeparator() +
							"  - script: unexpected error: " + e.getMessage());
				}
			}
		} catch (IOException e) {
			Log.error(e, "Failed to list scripts at '{}'", scriptsDir);
			return;
		}

		if (Log.isDebug() && !loaded.isEmpty()) {
			Log.debug("\n" + DebugPrinter.printGrid(loaded));
		}

		Summary.scriptsSummary(loaded.size(), enabled.size(), disabled.size(), errors);
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

	private static boolean isYaml(Path p) {
		if (!Files.isRegularFile(p))
			return false;
		String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
		return n.endsWith(".yml") || n.endsWith(".yaml");
	}
}
