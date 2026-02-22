package dev.objz.commandbridge.backends.platform;

import java.nio.file.Path;
import java.util.Locale;

public final class PathsUtil {
    private PathsUtil() {
    }

    public static Path normalizeDataDir(Path p) {
        if (p == null)
            return null;
        Path parent = p.getParent();
        if (parent == null)
            return p;
        String lower = p.getFileName().toString().toLowerCase(Locale.ROOT);
        return parent.resolve(lower);
    }
}
