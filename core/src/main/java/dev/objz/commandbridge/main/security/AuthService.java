package dev.objz.commandbridge.main.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class AuthService {
	private final byte[] key;

	public AuthService(String shared) {
		this.key = shared.getBytes(StandardCharsets.UTF_8);
	}

	public boolean verifyShared(String providedSecret) {
		if (providedSecret == null)
			return false;
		byte[] provided = providedSecret.getBytes(StandardCharsets.UTF_8);
		return constantTimeEquals(this.key, provided);
	}

	private static boolean constantTimeEquals(byte[] a, byte[] b) {
		if (a == null || b == null || a.length != b.length)
			return false;
		int r = 0;
		for (int i = 0; i < a.length; i++)
			r |= (a[i] ^ b[i]);
		return r == 0;
	}

	public String sign(String clientId, String nonce) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(key, "HmacSHA256"));
			byte[] out = mac.doFinal((clientId + ":" + nonce).getBytes(StandardCharsets.UTF_8));
			return Base64.getEncoder().encodeToString(out);
		} catch (Exception e) {
			throw new IllegalStateException("HMAC failure", e);
		}
	}

	public boolean verify(String clientId, String nonce, String macB64) {
		return sign(clientId, nonce).equals(macB64);
	}
}
