package dev.objz.commandbridge.security;

import dev.objz.commandbridge.TestFixtures;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TlsPinTest {

    @BeforeAll
    static void setUp() {
        TestFixtures.ensureLog();
    }

    @Test
    void pinMatchesTheSelfSignedServerPin(@TempDir Path dir) throws Exception {
        TLS.ensure(dir, "proxy");
        String password = Files.readString(dir.resolve("keystore.pass")).trim();

        String pin = TLS.keystoreSpkiPin(dir.resolve("keystore.p12").toString(), "PKCS12", password);

        assertNotNull(pin);
        assertTrue(pin.startsWith("sha256/"), pin);
        assertEquals(TLS.readServerSpkiPin(dir), pin);
    }

    @Test
    void wrongPasswordIsReported(@TempDir Path dir) {
        TLS.ensure(dir, "proxy");

        var e = assertThrows(IllegalStateException.class,
                () -> TLS.keystoreSpkiPin(dir.resolve("keystore.p12").toString(), "PKCS12", "nope"));
        assertTrue(e.getMessage().startsWith("Could not read the keystore: "), e.getMessage());
    }
}
