package dev.objz.commandbridge.backends;

import org.bukkit.plugin.java.JavaPlugin;

public final class PlatformResolver {
    private PlatformResolver() {}

    public static PlatformInterface detect(JavaPlugin plugin) {
        boolean isFolia = classPresent("io.papermc.paper.threadedregions.RegionizedServer");
        String implClassName = isFolia
                ? "dev.objz.commandbridge.backends.folia.Main"
                : "dev.objz.commandbridge.backends.bukkit.Main";

        try {
            Class<?> clazz = Class.forName(implClassName);
            return (PlatformInterface) clazz
                    .getConstructor(JavaPlugin.class)
                    .newInstance(plugin);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot load platform implementation: " + implClassName, e);
        }
    }

    private static boolean classPresent(String cn) {
        try { Class.forName(cn, false, PlatformResolver.class.getClassLoader()); return true; }
        catch (ClassNotFoundException e) { return false; }
    }
}
