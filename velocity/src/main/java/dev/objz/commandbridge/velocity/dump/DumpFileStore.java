package dev.objz.commandbridge.velocity.dump;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public final class DumpFileStore {

    private static final DateTimeFormatter FILE_TS =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC);

    private final Path dumpsDir;

    public DumpFileStore(Path dataDir) {
        this.dumpsDir = dataDir.resolve("dumps");
    }

    public Path write(String jsonPayload) throws IOException {
        Files.createDirectories(dumpsDir);
        String ts = FILE_TS.format(Instant.now());
        Path out = dumpsDir.resolve("dump-" + ts + ".json");
        Files.writeString(out, jsonPayload, StandardCharsets.UTF_8);
        return out;
    }
}
