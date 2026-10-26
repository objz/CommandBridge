package dev.objz.commandbridge.security;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Messages between the proxy and a page on cb.objz.dev. The key comes from the secret in the link's #fragment,
 * which the relay never sees, so it can neither read nor change them. The sender's role, the page's connection
 * id and the sequence number are bound in, so a message can't be reflected, moved to another tab or replayed.
 * The page does the same with WebCrypto (relay.js).
 */
public final class RelayCrypto {
    private static final byte[] SALT = "commandbridge-relay-v1".getBytes(StandardCharsets.UTF_8);
    private static final byte[] INFO = "aes-256-gcm".getBytes(StandardCharsets.UTF_8);
    private static final SecureRandom RANDOM = new SecureRandom();

    public record Sealed(String iv, String box) {
    }

    private RelayCrypto() {
    }

    public static SecretKeySpec key(byte[] secret) throws GeneralSecurityException {
        return new SecretKeySpec(PairCrypto.hkdf32(secret, SALT, INFO), "AES");
    }

    public static Sealed seal(SecretKeySpec key, String role, String conn, long seq, String body) throws GeneralSecurityException {
        byte[] iv = new byte[12];
        RANDOM.nextBytes(iv);
        return seal(key, role, conn, seq, iv, body);
    }

    static Sealed seal(SecretKeySpec key, String role, String conn, long seq, byte[] iv, String body) throws GeneralSecurityException {
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
        c.updateAAD(aad(role, conn, seq));
        byte[] box = c.doFinal(body.getBytes(StandardCharsets.UTF_8));
        Base64.Encoder b64 = Base64.getUrlEncoder().withoutPadding();
        return new Sealed(b64.encodeToString(iv), b64.encodeToString(box));
    }

    public static String open(SecretKeySpec key, String role, String conn, long seq, String iv, String box) throws GeneralSecurityException {
        byte[] ivBytes;
        byte[] boxBytes;
        try {
            ivBytes = Base64.getUrlDecoder().decode(iv);
            boxBytes = Base64.getUrlDecoder().decode(box);
        } catch (IllegalArgumentException e) {
            throw new GeneralSecurityException("Not base64", e);
        }
        if (ivBytes.length != 12)
            throw new GeneralSecurityException("Bad nonce length");
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, ivBytes));
        c.updateAAD(aad(role, conn, seq));
        return new String(c.doFinal(boxBytes), StandardCharsets.UTF_8);
    }

    private static byte[] aad(String role, String conn, long seq) {
        return (role + "\n" + conn + "\n" + seq).getBytes(StandardCharsets.UTF_8);
    }
}
