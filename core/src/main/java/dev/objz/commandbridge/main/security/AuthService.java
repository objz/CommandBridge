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

	public String sign(String clientId, String clientNonce) {
		return hmac(clientId + ":" + clientNonce);
	}

	public boolean verify(String clientId, String clientNonce, String macB64) {
		return sign(clientId, clientNonce).equals(macB64);
	}

	public String signServerProof(String clientId, String clientNonce, String serverNonce) {
		return hmac(clientId + ":" + clientNonce + ":" + serverNonce);
	}

	public boolean verifyServerProof(String clientId, String clientNonce, String serverNonce, String macB64) {
		return signServerProof(clientId, clientNonce, serverNonce).equals(macB64);
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
