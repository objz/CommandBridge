package dev.objz.commandbridge.security;

import dev.objz.commandbridge.TestFixtures;
import dev.objz.commandbridge.config.model.TlsMode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class TrustManagerTest {

    private static X509Certificate certA;
    private static X509Certificate certB;
    private static String pinA;
    private static String pinB;

    @TempDir
    static Path tmp;

    @BeforeAll
    static void setUp() throws Exception {
        TestFixtures.ensureLog();
        Path dirA = tmp.resolve("a");
        Path dirB = tmp.resolve("b");
        certA = generate(dirA);
        certB = generate(dirB);
        pinA = TLS.readServerSpkiPin(dirA);
        pinB = TLS.readServerSpkiPin(dirB);
        assertNotNull(pinA);
        assertNotEquals(pinA, pinB);
    }

    private static X509Certificate generate(Path dir) throws Exception {
        TLS.ensure(dir, "x");
        String password = Files.readString(dir.resolve("keystore.pass"), StandardCharsets.UTF_8).trim();
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (InputStream in = Files.newInputStream(dir.resolve("keystore.p12"))) {
            ks.load(in, password.toCharArray());
        }
        return (X509Certificate) ks.getCertificate("commandbridge");
    }

    private static X509Certificate[] chain(X509Certificate cert) {
        return new X509Certificate[] { cert };
    }

    @Test
    void strictAcceptsMatchingPin() {
        var tm = new TrustManager(TlsMode.STRICT, pinA);
        assertDoesNotThrow(() -> tm.checkServerTrusted(chain(certA), "RSA"));
    }

    @Test
    void strictRejectsMismatchedPin() {
        var tm = new TrustManager(TlsMode.STRICT, pinA);
        assertThrows(CertificateException.class, () -> tm.checkServerTrusted(chain(certB), "RSA"));
    }

    @Test
    void strictRejectsBlankPin() {
        var tm = new TrustManager(TlsMode.STRICT, "  ");
        assertThrows(CertificateException.class, () -> tm.checkServerTrusted(chain(certA), "RSA"));
    }

    @Test
    void strictRejectsMissingPin() {
        var tm = new TrustManager(TlsMode.STRICT, null);
        assertThrows(CertificateException.class, () -> tm.checkServerTrusted(chain(certA), "RSA"));
    }

    @Test
    void configuredPinIsTrimmed() {
        var tm = new TrustManager(TlsMode.STRICT, "  " + pinA + "\n");
        assertDoesNotThrow(() -> tm.checkServerTrusted(chain(certA), "RSA"));
    }

    @Test
    void tofuWithoutPinTrustsFirstCertificateAndKeepsTrustingIt() {
        var tm = new TrustManager(TlsMode.TOFU, "");
        assertDoesNotThrow(() -> tm.checkServerTrusted(chain(certA), "RSA"));
        assertDoesNotThrow(() -> tm.checkServerTrusted(chain(certA), "RSA"));
    }

    @Test
    void tofuWithoutPinRejectsDifferentCertificateAfterPinning() {
        var tm = new TrustManager(TlsMode.TOFU, null);
        assertDoesNotThrow(() -> tm.checkServerTrusted(chain(certA), "RSA"));
        assertThrows(CertificateException.class, () -> tm.checkServerTrusted(chain(certB), "RSA"));
    }

    @Test
    void tofuWithConfiguredPinRejectsDifferentCertificateOnFirstConnect() {
        var tm = new TrustManager(TlsMode.TOFU, pinA);
        assertThrows(CertificateException.class, () -> tm.checkServerTrusted(chain(certB), "RSA"));
    }

    @Test
    void tofuWithConfiguredPinAcceptsMatchingCertificate() {
        var tm = new TrustManager(TlsMode.TOFU, " " + pinB + " ");
        assertDoesNotThrow(() -> tm.checkServerTrusted(chain(certB), "RSA"));
    }

    @Test
    void emptyChainIsRejectedInEveryMode() {
        for (TlsMode mode : TlsMode.values()) {
            var tm = new TrustManager(mode, pinA);
            assertThrows(CertificateException.class,
                    () -> tm.checkServerTrusted(new X509Certificate[0], "RSA"), mode.name());
            assertThrows(CertificateException.class, () -> tm.checkServerTrusted(null, "RSA"), mode.name());
        }
    }

    @Test
    void plainAcceptsAnyCertificate() {
        var tm = new TrustManager(TlsMode.PLAIN, pinA);
        assertDoesNotThrow(() -> tm.checkServerTrusted(chain(certB), "RSA"));
    }
}
