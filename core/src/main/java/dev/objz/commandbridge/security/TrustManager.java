package dev.objz.commandbridge.security;

import javax.net.ssl.X509TrustManager;
import java.security.MessageDigest;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Objects;
import dev.objz.commandbridge.config.model.TlsMode;
import dev.objz.commandbridge.logging.Log;

public final class TrustManager implements X509TrustManager {
	private final TlsMode mode;
	private volatile String expectedPin;
	private final String configuredPin;

	public TrustManager(TlsMode mode, String configuredPin) {
		this.mode = Objects.requireNonNull(mode);
		this.configuredPin = sanitize(configuredPin);
		this.expectedPin = this.configuredPin;
	}

	@Override
	public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		// not used
	}

	@Override
	public X509Certificate[] getAcceptedIssuers() {
		return new X509Certificate[0];
	}

	@Override
	public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		if (chain == null || chain.length == 0) {
			Log.error("TLS validation failed: empty server certificate chain");
			throw new CertificateException("empty server certificate chain");
		}

		final String spki = spkiSha256(chain[0]);

		switch (mode) {
			case STRICT -> handleStrict(spki);
			case TOFU -> handleTofu(spki);
			case PLAIN -> Log.debug("TLS mode PLAIN: skipping SPKI pin validation");
		}
	}

	private void handleStrict(String spki) throws CertificateException {
		if (expectedPin == null || expectedPin.isBlank()) {
			Log.error("TLS STRICT mode requires a configured pin but none provided");
			throw new CertificateException("STRICT mode requires a TLS pin");
		}
		if (!expectedPin.equals(spki)) {
			Log.error("TLS pin mismatch (STRICT): expected '{}' but got '{}'", expectedPin, spki);
			throw new CertificateException("TLS pin mismatch");
		}
		Log.debug("TLS STRICT validation succeeded with pin '{}'", expectedPin);
	}

	private void handleTofu(String spki) throws CertificateException {
		if (configuredPin != null && !configuredPin.isBlank()) {
			if (!configuredPin.equals(spki)) {
				Log.error("TLS pin mismatch (TOFU configured): expected '{}' but got '{}'",
						configuredPin, spki);
				throw new CertificateException("TOFU (configured) pin mismatch");
			}
			Log.debug("TLS TOFU (configured) validation succeeded");
			return;
		}

		final String stored = expectedPin;
		if (stored == null || stored.isBlank()) {
			expectedPin = spki;
			Log.success(true, "Pinned TLS SPKI for this session: {}", spki);
		} else if (!stored.equals(spki)) {
			Log.error("TLS pin mismatch (TOFU session): expected '{}' but got '{}'", stored, spki);
			throw new CertificateException("TOFU session pin mismatch");
		} else {
			Log.debug("TLS TOFU validation succeeded with session pin '{}'", stored);
		}
	}

	private static String spkiSha256(X509Certificate cert) throws CertificateException {
		try {
			byte[] spki = cert.getPublicKey().getEncoded();
			byte[] sha256 = MessageDigest.getInstance("SHA-256").digest(spki);
			return "sha256/" + Base64.getEncoder().encodeToString(sha256);
		} catch (Exception e) {
			Log.error("Failed to compute SPKI digest: {}", e.getMessage());
			throw new CertificateException("SPKI digest failed");
		}
	}

	private static String sanitize(String s) {
		return s == null ? null : s.trim();
	}
}
