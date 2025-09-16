package dev.objz.commandbridge.velocity.scripting;

import dev.objz.commandbridge.main.logging.Log;
import dev.objz.commandbridge.main.logging.StatusLog;
import dev.objz.commandbridge.main.scripting.Effective;
import dev.objz.commandbridge.main.scripting.Schema;
import dev.objz.commandbridge.main.scripting.ScriptLoader;
import dev.objz.commandbridge.main.scripting.ScriptResolver;
import dev.objz.commandbridge.main.scripting.ScriptTypes.ScriptKind.Side;
import dev.objz.commandbridge.main.scripting.model.Spec;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public final class ScriptManager {

	public enum Status {
		ENABLED, DISABLED, ERROR
	}

	public static final class Entry {
		private final Status status;
		private final String name;
		private final Effective.Script script;
		private final String error;

		Entry(Status status, String name, Effective.Script script, String error) {
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

		public Optional<Effective.Script> script() {
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

	public static ScriptManager loadForSide(Path scriptsDir, Side side) {
		var registry = new Schema.Registry();
		var resolver = new ScriptResolver(registry);
		var loader = new ScriptLoader();

		List<Entry> out = new ArrayList<>();
		List<Spec.ScriptSpecV2> specs;

		try {
			specs = loader.loadAll(scriptsDir);
		} catch (IOException e) {
			out.add(new Entry(Status.ERROR, "<system>", null, "failed to read scripts: " + e.getMessage()));
			return new ScriptManager(out, 0);
		}

		for (Spec.ScriptSpecV2 spec : specs) {
			if (spec == null) {
				out.add(new Entry(Status.ERROR, "<unnamed>", null, "parse failure (null spec)"));
				continue;
			}

			if (spec.kind() == null || !spec.kind().registersOn(side)) {
				String nm = nonBlankOr(spec.name(), "<unnamed>");
				out.add(new Entry(Status.DISABLED, nm, null, null));
				continue;
			}

			List<String> errs = new ArrayList<>();
			require("version", spec.version(), errs);
			require("kind", spec.kind(), errs);
			require("name", spec.name(), errs);
			require("commands", spec.commands(), errs);

			if (!errs.isEmpty()) {
				String nm = nonBlankOr(spec.name(), "<unnamed>");
				out.add(new Entry(Status.ERROR, nm, null, String.join("\n", errs)));
				continue;
			}

			try {
				final Effective.Script eff = resolver.resolve(spec);
				if (eff.enabled()) {
					out.add(new Entry(Status.ENABLED, eff.name(), eff, null));
				} else {
					out.add(new Entry(Status.DISABLED, eff.name(), eff, null));
				}
			} catch (ScriptResolver.ValidationException vex) {
				String nm = nonBlankOr(spec.name(), "<unnamed>");
				out.add(new Entry(Status.ERROR, nm, null, String.join("\n", vex.errors())));
			} catch (Exception ex) {
				String nm = nonBlankOr(spec.name(), "<unnamed>");
				out.add(new Entry(Status.ERROR, nm, null, "resolution failed: " + ex.getMessage()));
			}
		}

		return new ScriptManager(out, specs.size());
	}

	public List<Entry> entries() {
		return entries;
	}

	public List<Effective.Script> enabled() {
		return entries.stream()
				.filter(e -> e.status == Status.ENABLED && e.script != null)
				.map(e -> e.script)
				.collect(Collectors.toUnmodifiableList());
	}

	public List<Effective.Script> disabled() {
		return entries.stream()
				.filter(e -> e.status == Status.DISABLED && e.script != null)
				.map(e -> e.script)
				.collect(Collectors.toUnmodifiableList());
	}

	public List<Entry> errors() {
		return entries.stream()
				.filter(e -> e.status == Status.ERROR)
				.collect(Collectors.toUnmodifiableList());
	}

	public void logReport(Path scriptsDir, boolean includeFileList) {
		List<Entry> errs = errors();
		for (Entry e : errs) {
			String header = "Script '" + e.name() + "' invalid:";
			String details = e.error()
					.map(ScriptManager::formatBulleted)
					.orElse("    - <unknown error>");
			Log.error("{}\n{}", header, details);
		}

		long en = entries.stream().filter(e -> e.status == Status.ENABLED).count();
		long dis = entries.stream().filter(e -> e.status == Status.DISABLED).count();
		long err = errs.size();

		StatusLog.scriptsSummary(loadedCount, en, dis, err);

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
		return Arrays.stream(lines)
				.map(String::trim)
				.filter(s -> !s.isEmpty())
				.map(s -> "    - " + s)
				.collect(Collectors.joining(System.lineSeparator()));
	}

	private static void require(String field, Object value, List<String> errs) {
		if (value == null) {
			errs.add(field + " is required");
			return;
		}
		if (value instanceof String s && s.isBlank()) {
			errs.add(field + " is required");
		} else if (value instanceof Collection<?> c && c.isEmpty()) {
			errs.add(field + " is required");
		}
	}

	private static String nonBlankOr(String v, String fb) {
		return (v == null || v.isBlank()) ? fb : v;
	}
}
