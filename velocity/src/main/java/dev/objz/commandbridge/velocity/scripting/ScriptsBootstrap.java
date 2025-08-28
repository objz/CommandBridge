package dev.objz.commandbridge.velocity.scripting;

import dev.objz.commandbridge.main.logging.Log;

import java.io.InputStream;
import java.nio.file.*;

public final class ScriptsBootstrap {
	private ScriptsBootstrap() {
	}

	public static Path ensureWithDemo(Path dataDir) {
		Path scriptsDir = dataDir.resolve("scripts");
		try {
			Files.createDirectories(scriptsDir);

			Path demoDst = scriptsDir.resolve("eco.yml");
			if (!Files.exists(demoDst)) {
				try (InputStream in = ScriptsBootstrap.class.getResourceAsStream("/scripts/eco.yml")) {
					if (in == null) {
						Log.warn("Demo resource '/scripts/eco.yml' is missing on the classpath");
					} else {
						Files.copy(in, demoDst);
						Log.success("Generated demo script at {}", demoDst.toAbsolutePath());
					}
				}
			}
		} catch (Exception e) {
			Log.warn("Failed to initialize scripts dir '{}': {}", scriptsDir, e.toString());
		}
		return scriptsDir;
	}
}
