package dev.objz.commandbridge.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

public final class AuthService {
    private final byte[] key;

    public AuthService(String shared) {
        this.key = shared.getBytes(StandardCharsets.UTF_8);
    }

    public String sign(String clientId, String clientNonce) {
        return hmac(clientId + ":" + clientNonce);
    }

    public boolean verify(String clientId, String clientNonce, String macB64) {
        return constantTimeEquals(sign(clientId, clientNonce), macB64);
    }

    public String signServerProof(String clientId, String clientNonce, String serverNonce) {
        return hmac(clientId + ":" + clientNonce + ":" + serverNonce);
    }

    public boolean verifyServerProof(String clientId, String clientNonce, String serverNonce, String macB64) {
        return constantTimeEquals(signServerProof(clientId, clientNonce, serverNonce), macB64);
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8));
    }

    private String hmac(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            byte[] out = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC failure", e);
        }
    }
}
