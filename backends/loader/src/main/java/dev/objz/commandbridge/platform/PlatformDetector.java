package dev.objz.commandbridge.platform;

import java.lang.reflect.Constructor;
import java.util.Optional;

public final class PlatformDetector {

	public enum Platform {
		FOLIA, PAPER, BUKKIT, FORGE, FABRIC, UNKNOWN
	}

	private PlatformDetector() {
	}

	/**
	 * Detection with strict priority inside the Bukkit family:
	 * FOLIA → PAPER → SPIGOT
	 */
	public static Platform detectPlatform() {
		if (classExists("io.papermc.paper.threadedregions.RegionizedServer"))
			return Platform.FOLIA;
		if (classExists("com.destroystokyo.paper.PaperConfig") || classExists("io.papermc.paper.PaperConfig"))
			return Platform.PAPER;
		if (classExists("org.bukkit.plugin.java.JavaPlugin"))
			return Platform.BUKKIT;

		if (classExists("net.minecraftforge.fml.ModList"))
			return Platform.FORGE;
		if (classExists("net.fabricmc.loader.api.FabricLoader"))
			return Platform.FABRIC;

		return Platform.UNKNOWN;
	}

	private static boolean classExists(String fqcn) {
		try {
			Class.forName(fqcn, false, PlatformDetector.class.getClassLoader());
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}

	/**
	 * Adapter resolution:
	 * Handles Bukkit-family explicitly with fixed priority.
	 * Extend this switch for Fabric/Forge/etc. as needed.
	 */
	public static Optional<PlatformAdapter> loadAdapter() {
		Platform p = detectPlatform();
		String implClass = switch (p) {
			case FOLIA -> "dev.objz.commandbridge.folia.impl.Adapter";
			case PAPER -> "dev.objz.commandbridge.paper.impl.Adapter";
			case BUKKIT -> "dev.objz.commandbridge.bukkit.impl.Adapter";
			case FORGE -> "dev.objz.commandbridge.forge.impl.Adapter";
			case FABRIC -> "dev.objz.commandbridge.fabric.impl.Adapter";
			default -> null;
		};
		return (implClass == null) ? Optional.empty() : instantiate(implClass);
	}

	private static Optional<PlatformAdapter> instantiate(String implClass) {
		try {
			Class<?> cls = Class.forName(implClass, true, PlatformDetector.class.getClassLoader());
			if (!PlatformAdapter.class.isAssignableFrom(cls))
				return Optional.empty();
			Constructor<?> ctor = cls.getDeclaredConstructor();
			ctor.setAccessible(true);
			return Optional.of((PlatformAdapter) ctor.newInstance());
		} catch (Throwable t) {
			return Optional.empty();
		}
	}
}
