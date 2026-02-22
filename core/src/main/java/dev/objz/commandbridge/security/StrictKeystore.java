package dev.objz.commandbridge.security;

import dev.objz.commandbridge.logging.Log;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Enumeration;

public final class StrictKeystore {
    private StrictKeystore() {
    }

    public static SSLContext load(String path, String type, String password) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("keystore path is empty");
        }
        if (password == null) {
            throw new IllegalArgumentException("keystore password is null");
        }
        String t = (type == null || type.isBlank()) ? "PKCS12" : type;
        try {
            KeyStore ks = KeyStore.getInstance(t);
            try (InputStream in = Files.newInputStream(Path.of(path))) {
                ks.load(in, password.toCharArray());
            }
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks, password.toCharArray());

            SSLContext ssl = SSLContext.getInstance("TLS");
            ssl.init(kmf.getKeyManagers(), null, null);

            try {
                String spki = firstSpki(ks);
                if (spki != null)
                    Log.success("TLS SPKI pin: {}", spki);
            } catch (Exception ignored) {
                /* not fatal */ }

            return ssl;
        } catch (Exception e) {
            Log.error("Failed to load STRICT keystore (type={}, path={})", t, path);
            throw new IllegalStateException("STRICT TLS keystore load failed", e);
        }
    }

    private static String firstSpki(KeyStore ks) throws Exception {
        Enumeration<String> aliases = ks.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            if (ks.isKeyEntry(alias)) {
                X509Certificate cert = (X509Certificate) ks.getCertificate(alias);
                byte[] spki = MessageDigest.getInstance("SHA-256")
                        .digest(cert.getPublicKey().getEncoded());
                return "sha256/" + Base64.getEncoder().encodeToString(spki);
            }
        }
        return null;
    }
}
