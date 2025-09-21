package dev.objz.commandbridge.platform;

import java.lang.reflect.Constructor;
import java.util.Optional;

public final class PlatformDetector {
    public enum Platform { FOLIA, PAPER, SPIGOT, FORGE, FABRIC, UNKNOWN }

    public static Platform detectPlatform() {
        if (classExists("io.papermc.paper.threadedregions.region.RegionScheduler")) return Platform.FOLIA;
        if (classExists("com.destroystokyo.paper.PaperConfig") || classExists("io.papermc.paper.PaperConfig")) return Platform.PAPER;
        if (classExists("org.bukkit.plugin.java.JavaPlugin")) return Platform.SPIGOT;
        if (classExists("net.minecraftforge.fml.ModList")) return Platform.FORGE;
        if (classExists("net.fabricmc.loader.api.FabricLoader")) return Platform.FABRIC;
        return Platform.UNKNOWN;
    }

    private static boolean classExists(String fqcn) {
        try { Class.forName(fqcn, false, PlatformDetector.class.getClassLoader()); return true; }
        catch (ClassNotFoundException e) { return false; }
    }

    public static Optional<PlatformAdapter> loadAdapter() {
        Platform p = detectPlatform();
        String implClass = switch (p) {
            case FOLIA -> "dev.objz.commandbridge.folia.impl.Adapter";
            case PAPER -> "dev.objz.commandbridge.paper.impl.Adapter";
            case SPIGOT -> "dev.objz.commandbridge.bukkit.impl.Adapter";
            // case FORGE -> "dev.objz.commandbridge.backends.impl.forge.ForgeAdapter";
            // case FABRIC -> "dev.objz.commandbridge.backends.impl.fabric.FabricAdapter";
            default -> null;
        };
        if (implClass == null) return Optional.empty();
        try {
            Class<?> cls = Class.forName(implClass);
            if (!PlatformAdapter.class.isAssignableFrom(cls)) return Optional.empty();
            Constructor<?> ctor = cls.getDeclaredConstructor();
            ctor.setAccessible(true);
            return Optional.of((PlatformAdapter) ctor.newInstance());
        } catch (Throwable t) {
            return Optional.empty();
        }
    }
}
