package dev.objz.commandbridge.velocity.scripting;

import dev.objz.commandbridge.main.logging.Log;
import dev.objz.commandbridge.main.logging.StatusLog;
import dev.objz.commandbridge.main.scripting.v3.ScriptEngine;
import dev.objz.commandbridge.main.scripting.v3.effective.EffectiveModels;
import dev.objz.commandbridge.main.scripting.v3.enums.ScriptSide;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * v3 ScriptManager: loads scripts through the new ScriptEngine and exposes
 * enabled/disabled/error entries. No backward compatibility with v2.
 */
public final class ScriptManager {

	public enum Status {
		ENABLED, DISABLED, ERROR
	}

	public static final class Entry {
		private final Status status;
		private final String name;
		private final EffectiveModels.Script script;
		private final String error;

		Entry(Status status, String name, EffectiveModels.Script script, String error) {
			this.status = status;
			this.name = name;
			this.script = script;
			this.error = error;
		}

		public Status status() {
			return status;
		}

		public String name() {
			return name;
		}

		public Optional<EffectiveModels.Script> script() {
			return Optional.ofNullable(script);
		}

		public Optional<String> error() {
			return Optional.ofNullable(error);
		}
	}

	private final List<Entry> entries;
	private final int loadedCount;

	private ScriptManager(List<Entry> entries, int loadedCount) {
		this.entries = List.copyOf(entries);
		this.loadedCount = loadedCount;
	}

	public static ScriptManager loadForSide(Path scriptsDir, ScriptSide side) {
		Objects.requireNonNull(side, "side");
		var engine = new ScriptEngine();

		List<Path> files = List.of();
		try {
			if (Files.isDirectory(scriptsDir)) {
				files = Files.list(scriptsDir)
						.filter(Files::isRegularFile)
						.filter(p -> {
							String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
							return n.endsWith(".yml") || n.endsWith(".yaml");
						})
						.sorted()
						.toList();
			}
		} catch (IOException ioe) {
			Log.error("Failed to list scripts dir '{}': {}", scriptsDir, ioe.toString());
		}

		List<Entry> out = new ArrayList<>();
		for (Path f : files) {
			EffectiveModels.Script s = null;
			try {
				s = engine.load(f);
			} catch (Exception e) {
				String nm = dropExt(f.getFileName().toString());
				out.add(new Entry(Status.ERROR, nm, null, e.getMessage()));
				continue;
			}
			if (s == null) {
				String nm = dropExt(f.getFileName().toString());
				out.add(new Entry(Status.ERROR, nm, null, "could not read this YAML file"));
				continue;
			}

			if (s.defaults() == null || s.defaults().target() == null) {
				out.add(new Entry(Status.ERROR, s.name(), null, "missing defaults/target"));
				continue;
			}
			if (s.defaults().target().register() != side) {
				out.add(new Entry(Status.DISABLED, s.name(), s, null));
				continue;
			}

			if (s.enabled()) {
				out.add(new Entry(Status.ENABLED, s.name(), s, null));
			} else {
				out.add(new Entry(Status.DISABLED, s.name(), s, null));
			}
		}

		return new ScriptManager(out, files.size());
	}

	public List<Entry> entries() {
		return entries;
	}

	public List<EffectiveModels.Script> enabled() {
		return entries.stream().filter(e -> e.status == Status.ENABLED && e.script != null)
				.map(e -> e.script).collect(Collectors.toUnmodifiableList());
	}

	public List<EffectiveModels.Script> disabled() {
		return entries.stream().filter(e -> e.status == Status.DISABLED && e.script != null)
				.map(e -> e.script).collect(Collectors.toUnmodifiableList());
	}

	public List<Entry> errors() {
		return entries.stream().filter(e -> e.status == Status.ERROR).collect(Collectors.toUnmodifiableList());
	}

	public void logReport(Path scriptsDir, boolean includeFileList) {
		List<Entry> errs = errors();
		for (Entry e : errs) {
			String header = "Script '" + e.name() + "' invalid:";
			String details = e.error().map(ScriptManager::formatBulleted).orElse("    - <unknown error>");
			Log.error("{}\n{}", header, details);
		}

		long en = entries.stream().filter(e -> e.status == Status.ENABLED).count();
		long dis = entries.stream().filter(e -> e.status == Status.DISABLED).count();
		long err = errs.size();
		StatusLog.scriptsSummary(loadedCount, en, dis + err, err);

		if (includeFileList) {
			try {
				if (Files.isDirectory(scriptsDir)) {
					String names = Files.list(scriptsDir)
							.filter(Files::isRegularFile)
							.map(p -> p.getFileName().toString())
							.sorted()
							.collect(Collectors.joining(", "));
					Log.debug("Scripts present: {}", names);
				}
			} catch (Exception ioe) {
				Log.warn("Could not list scripts in {}: {}", scriptsDir, ioe.toString());
			}
		}
	}

	private static String formatBulleted(String raw) {
		String[] lines = raw.split("\\r?\\n|;\\s*");
		return Arrays.stream(lines).map(String::trim).filter(s -> !s.isEmpty())
				.map(s -> "    - " + s).collect(Collectors.joining(System.lineSeparator()));
	}

	private static String dropExt(String n) {
		int i = n.lastIndexOf('.');
		return (i > 0) ? n.substring(0, i) : n;
	}
}
