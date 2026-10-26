package dev.objz.commandbridge.security;

import org.junit.jupiter.api.Test;

import java.security.GeneralSecurityException;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RelayCryptoTest {

    private static final byte[] SECRET = new byte[32];
    static {
        for (int i = 0; i < 32; i++)
            SECRET[i] = (byte) i;
    }

    private static String b64(byte[] b) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    // the same values the page's WebCrypto code produces, so both ends agree byte for byte
    @Test
    void matchesTheBrowserVector() throws Exception {
        assertEquals("EUNfwa2OZzhL3FajxNYm7dXE4FjziH34xDuTYvlkV6k", b64(RelayCrypto.key(SECRET).getEncoded()));
        byte[] iv = new byte[12];
        for (int i = 0; i < 12; i++)
            iv[i] = (byte) (100 + i);

        RelayCrypto.Sealed sealed = RelayCrypto.seal(RelayCrypto.key(SECRET), "editor", "c0ffee", 7, iv, "{\"type\":\"hello\"}");

        assertEquals("ZGVmZ2hpamtsbW5v", sealed.iv());
        assertEquals("PDH78F261Itf5oEthyeLevDaXeUgPKqBC6W6JUhJMqw", sealed.box());
    }

    @Test
    void opensWhatItSealed() throws Exception {
        var key = RelayCrypto.key(SECRET);
        RelayCrypto.Sealed s = RelayCrypto.seal(key, "proxy", "abc", 3, "{\"type\":\"state\"}");

        assertEquals("{\"type\":\"state\"}", RelayCrypto.open(key, "proxy", "abc", 3, s.iv(), s.box()));
    }

    @Test
    void refusesAnotherRoleConnectionOrSequence() throws Exception {
        var key = RelayCrypto.key(SECRET);
        RelayCrypto.Sealed s = RelayCrypto.seal(key, "proxy", "abc", 3, "x");

        assertThrows(GeneralSecurityException.class, () -> RelayCrypto.open(key, "editor", "abc", 3, s.iv(), s.box()));
        assertThrows(GeneralSecurityException.class, () -> RelayCrypto.open(key, "proxy", "abd", 3, s.iv(), s.box()));
        assertThrows(GeneralSecurityException.class, () -> RelayCrypto.open(key, "proxy", "abc", 4, s.iv(), s.box()));
    }

    @Test
    void refusesTamperedOrBrokenInput() throws Exception {
        var key = RelayCrypto.key(SECRET);
        RelayCrypto.Sealed s = RelayCrypto.seal(key, "proxy", "abc", 3, "x");
        String flipped = (s.box().charAt(0) == 'A' ? "B" : "A") + s.box().substring(1);

        assertThrows(GeneralSecurityException.class, () -> RelayCrypto.open(key, "proxy", "abc", 3, s.iv(), flipped));
        assertThrows(GeneralSecurityException.class, () -> RelayCrypto.open(key, "proxy", "abc", 3, "not base64!", s.box()));
        assertThrows(GeneralSecurityException.class, () -> RelayCrypto.open(key, "proxy", "abc", 3, "AAAA", s.box()));
    }
}
