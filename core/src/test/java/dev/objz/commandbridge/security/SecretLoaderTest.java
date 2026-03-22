package dev.objz.commandbridge.security;

import dev.objz.commandbridge.logging.Log;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecretLoaderTest {

    @BeforeAll
    static void installLog() {
        try {
            Log.install(Logger.getLogger("test"));
        } catch (Exception e) {
            // Log already installed or unavailable
        }
    }

    @Test
    void loadOrCreateGeneratesNewKeyWhenFileAbsent(@TempDir Path tempDir) {
        SecretLoader loader = new SecretLoader(tempDir);
        String result = loader.loadOrCreate();

        assertNotNull(result);
        assertTrue(!result.isBlank());
    }

    @Test
    void loadOrCreateReadsExistingKey(@TempDir Path tempDir) throws Exception {
        Path secretFile = tempDir.resolve("secret.key");
        Files.createDirectories(tempDir);
        Files.writeString(secretFile, "mysecret", StandardCharsets.UTF_8);

        SecretLoader loader = new SecretLoader(tempDir);
        String result = loader.loadOrCreate();

        assertEquals("mysecret", result);
    }

    @Test
    void generatedKeyIsUrlSafeBase64(@TempDir Path tempDir) {
        SecretLoader loader = new SecretLoader(tempDir);
        String result = loader.loadOrCreate();

        assertTrue(result.matches("[A-Za-z0-9_=-]+"));
    }

    @Test
    void loadedKeyIsTrimmed(@TempDir Path tempDir) throws Exception {
        Path secretFile = tempDir.resolve("secret.key");
        Files.createDirectories(tempDir);
        Files.writeString(secretFile, "mysecret\n\n", StandardCharsets.UTF_8);

        SecretLoader loader = new SecretLoader(tempDir);
        String result = loader.loadOrCreate();

        assertEquals("mysecret", result);
    }
}
