package dev.objz.commandbridge.backends.platform;

public final class PlatformDetector {

    public enum Platform {
        FOLIA, PAPER, BUKKIT, FORGE, FABRIC, UNKNOWN
    }

    private PlatformDetector() {
    }

    public static Platform detectPlatform() {
        Platform p;
        if (classExists("io.papermc.paper.threadedregions.RegionizedServer")) {
            p = Platform.FOLIA;
        } else if (classExists("com.destroystokyo.paper.PaperConfig")
                || classExists("io.papermc.paper.configuration.Configuration")) {
            p = Platform.PAPER;
        } else if (classExists("org.bukkit.Bukkit")) {
            p = Platform.BUKKIT;
        } else if (classExists("net.minecraftforge.fml.common.Mod")) {
            p = Platform.FORGE;
        } else if (classExists("net.fabricmc.loader.api.FabricLoader")) {
            p = Platform.FABRIC;
        } else {
            p = Platform.UNKNOWN;
        }
        return p;
    }

    private static boolean classExists(String fqcn) {
        try {
            Class.forName(fqcn, false, PlatformDetector.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
