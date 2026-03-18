package dev.objz.commandbridge.security;

import dev.objz.commandbridge.logging.Log;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.security.SecureRandom;
import java.util.Base64;

public final class SecretLoader {
    private final Path secretFile;

    public SecretLoader(Path dataDir) {
        this.secretFile = dataDir.resolve("secret.key");
    }

    public String loadOrCreate() {
        try {
            Files.createDirectories(secretFile.getParent());
            if (!Files.exists(secretFile)) {
                String k = generate();
                Files.writeString(secretFile, k + System.lineSeparator(), StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE_NEW);
                try { // set 600 if possible
                    Files.setPosixFilePermissions(secretFile,
                            java.util.Set.of(PosixFilePermission.OWNER_READ,
                                    PosixFilePermission.OWNER_WRITE));
                } catch (UnsupportedOperationException ignored) {
                }
                Log.success(true, "Generated new secret at '{}'", secretFile);
                return k;
            }
            var key = Files.readString(secretFile, StandardCharsets.UTF_8).trim();
            if (key.isEmpty())
                throw new IllegalStateException("secret.key is empty");
            return key;
        } catch (IOException e) {
            Log.error(e, "Could not read/write {}", secretFile);
            throw new IllegalStateException("secret load failed");
        }
    }

    /**
     * Resolves the shared secret: uses the config value if present, otherwise loads/creates from file.
     */
    public static String resolve(String configSecret, Path dataDir) {
        if (configSecret != null && !configSecret.isBlank()) {
            Log.debug("Secret resolved from config value");
            return configSecret;
        }
        Log.debug("Secret resolved from file at {}", dataDir.resolve("secret.key"));
        return new SecretLoader(dataDir).loadOrCreate();
    }

    private static String generate() {
        byte[] raw = new byte[32];
        new SecureRandom().nextBytes(raw);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw); // 43 chars
    }
}
