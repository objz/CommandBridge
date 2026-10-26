package dev.objz.commandbridge.backends.platform;

import dev.objz.commandbridge.scripting.model.enums.ServerPlatform;

public final class PlatformDetector {

    private PlatformDetector() {
    }

    public static ServerPlatform detectPlatform() {
        ServerPlatform p;
        if (classExists("io.papermc.paper.threadedregions.RegionizedServer")) {
            p = ServerPlatform.FOLIA;
        } else if (classExists("com.destroystokyo.paper.PaperConfig")
                || classExists("io.papermc.paper.configuration.Configuration")) {
            p = ServerPlatform.PAPER;
        } else if (classExists("org.bukkit.Bukkit")) {
            p = ServerPlatform.BUKKIT;
        } else if (classExists("net.minecraftforge.fml.common.Mod")) {
            p = ServerPlatform.FORGE;
        } else if (classExists("net.fabricmc.loader.api.FabricLoader")) {
            p = ServerPlatform.FABRIC;
        } else {
            p = ServerPlatform.UNKNOWN;
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
