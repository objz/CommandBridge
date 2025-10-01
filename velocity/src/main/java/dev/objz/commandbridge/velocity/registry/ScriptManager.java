package dev.objz.commandbridge.velocity.registry;

import dev.objz.commandbridge.main.logging.Log;
import dev.objz.commandbridge.main.logging.StatusLog;
import dev.objz.commandbridge.main.scripting.v3.compiler.ScriptCompiler;
import dev.objz.commandbridge.main.scripting.v3.compiler.problems.Problem;
import dev.objz.commandbridge.main.scripting.v3.compiler.problems.Severity;
import dev.objz.commandbridge.main.scripting.v3.model.domain.Script;
import dev.objz.commandbridge.velocity.ScriptDebug;

import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.objectmapping.ObjectMapper;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Velocity-side simple registry backed by v3 ScriptLoader.
 * - Loads <dataDir>/scripts/** (recursive)
 * - Collects problems via sink (per file) and logs a compact invalid report
 * - Publishes overall summary via StatusLog.scriptsSummary(loaded, enabled,
 * disabled, errors)
 * - Keeps only "enabled & error-free" scripts in memory for registration
 */
public final class ScriptManager {
	private final ObjectMapper.Factory mapperFactory;
	private final Map<String, Script> enabled = new ConcurrentHashMap<>();

	// Pattern to fix Configurate's path formatting: [field, index, subfield] -> field[index].subfield
	private static final Pattern CONFIGURATE_PATH_PATTERN = Pattern.compile("\\[([^\\]]+)\\]");

	public ScriptManager(ObjectMapper.Factory mapperFactory) {
		this.mapperFactory = Objects.requireNonNull(mapperFactory, "mapperFactory");
	}

	/**
	 * Load/Reload all scripts from <dataDir>/scripts/**. Missing dir is created.
	 */
	public void loadAll(Path scriptsDir) {
		enabled.clear();

		try {
			if (Files.notExists(scriptsDir)) {
				Files.createDirectories(scriptsDir);
			}
		} catch (IOException ioe) {
			Log.error("Failed to create scripts dir '{}': {}", scriptsDir, ioe.toString());
			// summary with zeroes + one error
			StatusLog.scriptsSummary(0, 0, 0, 1);
			return;
		}

		List<Path> files;
		try (Stream<Path> s = Files.walk(scriptsDir)) {
			files = s.filter(Files::isRegularFile)
					.filter(p -> {
						var n = p.getFileName().toString().toLowerCase(Locale.ROOT);
						return n.endsWith(".yml") || n.endsWith(".yaml");
					})
					.sorted()
					.toList();
		} catch (IOException ioe) {
			Log.error("Scanning scripts failed: {}", ioe.toString());
			StatusLog.scriptsSummary(0, 0, 0, 1);
			return;
		}

		int total = files.size();
		int totalErrors = 0;
		int enabledCount = 0;
		int disabledCount = 0;

		var loader = new ScriptCompiler(mapperFactory);

		for (Path file : files) {
			String source = scriptsDir.relativize(file).toString();

			ConfigurationNode node;
			try {
				node = YamlConfigurationLoader.builder().path(file).build().load();
			} catch (Exception ex) {
				totalErrors++;
				disabledCount++;
				
				// Format load failed error hierarchically
				StringBuilder errorMsg = new StringBuilder();
				errorMsg.append("Script '").append(source).append("' invalid:");
				errorMsg.append("\n  - load failed:");
				errorMsg.append("\n       - ").append(ex.getMessage());
				
				Log.error(errorMsg.toString());
				continue;
			}

			ScriptCompiler.Result res;
			try {
				res = loader.load(node, source);
			} catch (SerializationException ex) {
				totalErrors++;
				disabledCount++;
				
				// Format compile failed error hierarchically
				StringBuilder errorMsg = new StringBuilder();
				errorMsg.append("Script '").append(source).append("' invalid:");
				errorMsg.append("\n  - compile failed:");
				
				// Parse and format the SerializationException
				String formattedError = formatConfigurateError(ex);
				errorMsg.append("\n       - ").append(formattedError);
				
				Log.error(errorMsg.toString());
				continue;
			} catch (Exception ex) {
				totalErrors++;
				disabledCount++;
				
				// Format other compile errors hierarchically
				StringBuilder errorMsg = new StringBuilder();
				errorMsg.append("Script '").append(source).append("' invalid:");
				errorMsg.append("\n  - compile failed:");
				errorMsg.append("\n       - ").append(ex.getMessage());
				
				Log.error(errorMsg.toString());
				continue;
			}

			var sink = res.sink;
			var script = res.script; // may be null if sink.hasErrors()

			if (sink.hasErrors() || script == null) {
				// Count individual errors, not just the script failure
				int errorCount = 0;
				int warningCount = 0;

				for (Problem p : sink.snapshot()) {
					if (p.severity() == Severity.ERROR) {
						errorCount++;
					} else {
						warningCount++;
					}
				}

				totalErrors += errorCount;
				disabledCount++;

				// Build the error message with consistent formatting
				StringBuilder errorMsg = new StringBuilder();
				errorMsg.append("Script '").append(source).append("' invalid:");

				for (Problem p : sink.snapshot()) {
					errorMsg.append("\n  - ").append(p.path()).append(": ").append(p.message());
				}

				Log.error(errorMsg.toString());
				continue;
			}

			// Script is syntactically valid; check if it should be enabled
			if (script.enabled()) {
				enabled.put(script.name(), script);
				enabledCount++;

				ScriptDebug.printScript(script, source);
			} else {
				disabledCount++;
			}

			// Log warnings if any (for valid scripts)
			for (Problem p : sink.snapshot()) {
				if (p.severity() == Severity.WARN) {
					Log.warn("Script '{}': {}: {}", source, p.path(), p.message());
				}
			}
		}

		StatusLog.scriptsSummary(total, enabledCount, disabledCount, totalErrors);
	}

	/**
	 * Format Configurate error messages to match our consistent hierarchical style.
	 * Converts SerializationException to: field[index].subfield: error message
	 */
	private static String formatConfigurateError(SerializationException ex) {
		String message = ex.getMessage();
		if (message == null) return "Unknown serialization error";
		
		// Extract the path and error message from SerializationException
		// Typical format: "[field, index, subfield] of type X: error message"
		String formattedPath = "unknown";
		String errorMessage = message;
		
		// Try to extract path from the message
		int pathStart = message.indexOf('[');
		int pathEnd = message.indexOf(']');
		int colonIndex = message.indexOf(':', pathEnd);
		
		if (pathStart >= 0 && pathEnd > pathStart && colonIndex > pathEnd) {
			// Extract and format the path
			String pathContent = message.substring(pathStart + 1, pathEnd);
			formattedPath = formatPath(pathContent);
			
			// Extract the error message (everything after "type X: ")
			String afterColon = message.substring(colonIndex + 1).trim();
			
			// Remove "of type X" part if present
			int typeIndex = message.indexOf(" of type ");
			if (typeIndex >= 0 && typeIndex < colonIndex) {
				errorMessage = afterColon;
			} else {
				errorMessage = afterColon;
			}
		}
		
		return formattedPath + ": " + errorMessage;
	}
	
	/**
	 * Convert Configurate path format to our format.
	 * "args, 0, required" -> "args[0].required"
	 */
	private static String formatPath(String pathContent) {
		String[] parts = pathContent.split(",\\s*");
		
		StringBuilder formatted = new StringBuilder();
		for (int i = 0; i < parts.length; i++) {
			String part = parts[i].trim();
			
			if (i == 0) {
				// First part is always a field name
				formatted.append(part);
			} else if (part.matches("\\d+")) {
				// Numeric parts become array indices
				formatted.append("[").append(part).append("]");
			} else {
				// Other parts become dot notation
				formatted.append(".").append(part);
			}
		}
		
		return formatted.toString();
	}

	/** Enabled & error-free scripts for registration. */
	public List<Script> enabled() {
		return List.copyOf(enabled.values());
	}

	public Optional<Script> get(String name) {
		return Optional.ofNullable(enabled.get(name));
	}
}
