package dev.objz.commandbridge.security;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PairCryptoTest {

    @Test
    void newCodeHasTenCharsInThreeGroups() {
        for (int i = 0; i < 50; i++) {
            String code = PairCrypto.newCode();
            assertTrue(code.matches("^[2-9A-HJKMNP-Z]{4}-[2-9A-HJKMNP-Z]{4}-[2-9A-HJKMNP-Z]{2}$"), code);
        }
    }

    @Test
    void normalizeAcceptsHowPeopleType() {
        assertEquals("7KQM4XR29W", PairCrypto.normalize("7kqm 4xr2-9w"));
    }

    @Test
    void normalizeRejectsWrongLength() {
        var e = assertThrows(IllegalArgumentException.class, () -> PairCrypto.normalize("7KQM-4XR2"));
        assertEquals("A pairing code has 10 characters", e.getMessage());
    }

    @Test
    void channelIdIsStableAndCaseFree() {
        String id = PairCrypto.channelId("7kqm-4xr2-9w");
        assertEquals(id, PairCrypto.channelId("7KQM4XR29W"));
        assertTrue(id.matches("^[0-9a-f]{64}$"), id);
    }

    @Test
    void sealOpensWithSameCode() throws Exception {
        String sealed = PairCrypto.seal("7KQM-4XR2-9W", "hello".getBytes(StandardCharsets.UTF_8));
        assertArrayEquals("hello".getBytes(StandardCharsets.UTF_8), PairCrypto.open("7kqm4xr29w", sealed));
    }

    @Test
    void openFailsWithOtherCode() throws Exception {
        String sealed = PairCrypto.seal("7KQM-4XR2-9W", "hello".getBytes(StandardCharsets.UTF_8));
        assertThrows(GeneralSecurityException.class, () -> PairCrypto.open("7KQM-4XR2-9X", sealed));
    }

    @Test
    void sealIsRandomized() throws Exception {
        byte[] in = "hello".getBytes(StandardCharsets.UTF_8);
        assertNotEquals(PairCrypto.seal("7KQM-4XR2-9W", in), PairCrypto.seal("7KQM-4XR2-9W", in));
    }
}
