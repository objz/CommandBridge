package dev.objz.commandbridge.security;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

// Pairing codes for /cbc pair: the code names the relay channel and is the only key to the sealed config.
public final class PairCrypto {
    private static final String ALPHABET = "23456789ABCDEFGHJKMNPQRSTUVWXYZ";
    private static final int LENGTH = 10;
    private static final byte[] SALT = "commandbridge-pair-v1".getBytes(StandardCharsets.UTF_8);
    private static final byte[] INFO = "config".getBytes(StandardCharsets.UTF_8);
    private static final int NONCE = 12;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PairCrypto() {
    }

    public static String newCode() {
        StringBuilder sb = new StringBuilder(LENGTH + 2);
        for (int i = 0; i < LENGTH; i++) {
            if (i == 4 || i == 8)
                sb.append('-');
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    public static String normalize(String typed) {
        StringBuilder sb = new StringBuilder(LENGTH);
        for (char c : typed.toUpperCase(Locale.ROOT).toCharArray()) {
            if (ALPHABET.indexOf(c) >= 0)
                sb.append(c);
        }
        if (sb.length() != LENGTH)
            throw new IllegalArgumentException("A pairing code has 10 characters");
        return sb.toString();
    }

    public static String channelId(String code) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(("cb-pair\n" + normalize(code)).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static String seal(String code, byte[] plaintext) throws GeneralSecurityException {
        byte[] nonce = new byte[NONCE];
        RANDOM.nextBytes(nonce);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key(code), new GCMParameterSpec(128, nonce));
        byte[] ct = cipher.doFinal(plaintext);
        return Base64.getEncoder().encodeToString(ByteBuffer.allocate(NONCE + ct.length).put(nonce).put(ct).array());
    }

    public static byte[] open(String code, String sealed) throws GeneralSecurityException {
        byte[] all;
        try {
            all = Base64.getDecoder().decode(sealed);
        } catch (IllegalArgumentException e) {
            throw new GeneralSecurityException("Sealed data is not base64", e);
        }
        if (all.length <= NONCE)
            throw new GeneralSecurityException("Sealed data is too short");
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key(code), new GCMParameterSpec(128, all, 0, NONCE));
        return cipher.doFinal(all, NONCE, all.length - NONCE);
    }

    private static SecretKeySpec key(String code) throws GeneralSecurityException {
        return new SecretKeySpec(hkdf32(normalize(code).getBytes(StandardCharsets.UTF_8), SALT, INFO), "AES");
    }

    // HKDF-SHA256 (RFC 5869), one expand block is enough for a 32-byte key.
    static byte[] hkdf32(byte[] ikm, byte[] salt, byte[] info) throws GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(salt, "HmacSHA256"));
        byte[] prk = mac.doFinal(ikm);
        mac.init(new SecretKeySpec(prk, "HmacSHA256"));
        mac.update(info);
        mac.update((byte) 1);
        return mac.doFinal();
    }
}
