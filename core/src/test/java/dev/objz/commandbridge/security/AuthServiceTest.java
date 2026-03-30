package dev.objz.commandbridge.security;

import dev.objz.commandbridge.TestFixtures;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link AuthService}.
 * Verifies HMAC-SHA256 signing determinism and constant-time verification correctness.
 */
final class AuthServiceTest {

    private static final String SECRET = "test-secret-key-for-testing-12345678";
    private static AuthService auth;

    @BeforeAll
    static void setUp() {
        TestFixtures.ensureLog();
        auth = new AuthService(SECRET);
    }

    @Test
    void signValidInputReturnsDeterministicOutput() {
        String first = auth.sign("client-1", "nonce-abc");
        String second = auth.sign("client-1", "nonce-abc");
        assertEquals(first, second);
        assertNotNull(first);
        assertFalse(first.isEmpty());
    }

    @Test
    void signDifferentClientIdReturnsDifferentOutput() {
        String a = auth.sign("client-a", "nonce-shared");
        String b = auth.sign("client-b", "nonce-shared");
        assertNotEquals(a, b);
    }

    @Test
    void signDifferentNonceReturnsDifferentOutput() {
        String a = auth.sign("client-1", "nonce-1");
        String b = auth.sign("client-1", "nonce-2");
        assertNotEquals(a, b);
    }

    @Test
    void verifyValidSignatureReturnsTrue() {
        String proof = auth.sign("client-1", "nonce-xyz");
        assertTrue(auth.verify("client-1", "nonce-xyz", proof));
    }

    @Test
    void verifyInvalidSignatureReturnsFalse() {
        assertFalse(auth.verify("client-1", "nonce-xyz", "dGhpcyBpcyBub3QgYSB2YWxpZCBobWFj"));
    }

    @Test
    void verifyTamperedClientIdReturnsFalse() {
        String proof = auth.sign("client-a", "nonce-123");
        assertFalse(auth.verify("client-b", "nonce-123", proof));
    }

    @Test
    void signServerProofValidInputReturnsDeterministicOutput() {
        String first = auth.signServerProof("client-1", "cnonce", "snonce");
        String second = auth.signServerProof("client-1", "cnonce", "snonce");
        assertEquals(first, second);
        assertNotNull(first);
        assertFalse(first.isEmpty());
    }

    @Test
    void verifyServerProofValidProofReturnsTrue() {
        String proof = auth.signServerProof("client-1", "cnonce", "snonce");
        assertTrue(auth.verifyServerProof("client-1", "cnonce", "snonce", proof));
    }

    @Test
    void verifyServerProofInvalidProofReturnsFalse() {
        assertFalse(auth.verifyServerProof("client-1", "cnonce", "snonce", "aW52YWxpZC1wcm9vZg=="));
    }
}
