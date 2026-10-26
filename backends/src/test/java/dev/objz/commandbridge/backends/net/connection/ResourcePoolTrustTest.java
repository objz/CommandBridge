package dev.objz.commandbridge.backends.net.connection;

import dev.objz.commandbridge.backends.TestFixtures;
import dev.objz.commandbridge.config.model.TlsMode;
import dev.objz.commandbridge.security.TrustManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class ResourcePoolTrustTest {
    @TempDir
    Path directory;

    @BeforeAll
    static void setup() {
        TestFixtures.ensureLog();
    }

    @Test
    void learnedTofuPinSurvivesResourceRecreation() throws Exception {
        X509Certificate[] first = certificate(directory.resolve("first"));
        X509Certificate[] replacement = certificate(directory.resolve("replacement"));
        Field trust = ResourcePool.class.getDeclaredField("sessionTrust");
        trust.setAccessible(true);
        try (ResourcePool pool = new ResourcePool()) {
            pool.initialize();
            pool.initializeSsl(TlsMode.TOFU, null, false);
            TrustManager learned = (TrustManager) trust.get(pool);
            learned.checkServerTrusted(first, "RSA");
            pool.close();

            pool.initialize();
            pool.initializeSsl(TlsMode.TOFU, null, true);
            TrustManager reconnected = (TrustManager) trust.get(pool);

            assertSame(learned, reconnected);
            assertDoesNotThrow(() -> reconnected.checkServerTrusted(first, "RSA"));
            assertThrows(CertificateException.class, () -> reconnected.checkServerTrusted(replacement, "RSA"));
        }
    }

    private X509Certificate[] certificate(Path path) throws Exception {
        Files.createDirectories(path);
        Process keytool = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "keytool").toString(),
                "-genkeypair", "-alias", "commandbridge", "-keyalg", "RSA", "-keysize", "2048",
                "-storetype", "PKCS12", "-keystore", path.resolve("keystore.p12").toString(),
                "-storepass", "test-password", "-dname", "CN=test")
                .redirectErrorStream(true).start();
        if (!keytool.waitFor(30, TimeUnit.SECONDS)) {
            keytool.destroyForcibly();
            throw new AssertionError("Test certificate generation timed out");
        }
        assertEquals(0, keytool.exitValue(), new String(keytool.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
        KeyStore store = KeyStore.getInstance("PKCS12");
        try (InputStream input = Files.newInputStream(path.resolve("keystore.p12"))) {
            store.load(input, "test-password".toCharArray());
        }
        return new X509Certificate[] { (X509Certificate) store.getCertificate("commandbridge") };
    }
}
