package dev.objz.commandbridge.security;

import dev.objz.commandbridge.logging.Log;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermission;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.Set;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public final class TLS {
	private static final String KS_NAME = "keystore.p12";
	private static final String PASS_NAME = "keystore.pass";
	private static final String ALIAS = "commandbridge";
	private static final String STORE_TYPE = "PKCS12";
	private static final String SIG_ALG = "SHA256withRSA";

	private TLS() {
	}

	public static SSLContext ensure(Path dataDir, String cnHint) {
		try {
			ensureBcProvider();
			Files.createDirectories(dataDir);
			Path ksPath = dataDir.resolve(KS_NAME);
			Path pwPath = dataDir.resolve(PASS_NAME);

			String password;
			if (Files.exists(ksPath) && Files.exists(pwPath)) {
				password = Files.readString(pwPath, StandardCharsets.UTF_8).trim();
				if (password.isEmpty())
					throw new IllegalStateException("empty keystore.pass");
			} else {
				password = randomPassword();
				writePassword(pwPath, password);
				String cn = (cnHint != null && !cnHint.isBlank()) ? cnHint : "localhost";
				generateKeystoreBc(ksPath, password, cn);
				Log.success(true, "Generated TLS keystore at '{}'", ksPath);
			}

			KeyStore ks = KeyStore.getInstance(STORE_TYPE);
			try (InputStream in = Files.newInputStream(ksPath)) {
				ks.load(in, password.toCharArray());
			}
			KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			kmf.init(ks, password.toCharArray());

			SSLContext ssl = SSLContext.getInstance("TLS");
			ssl.init(kmf.getKeyManagers(), null, null);

			try {
				String spki = spkiPinFromKeystore(ks, password);
				if (spki != null) {
					Log.success(true, "TLS SPKI pin: {}", spki);

				}
			} catch (Exception e) {
				Log.warn("Could not compute SPKI pin: {}", e.toString());
			}

			return ssl;

		} catch (Throwable e) {
			Log.warn("BC TLS bootstrap failed, trying keytool fallback: {}", e.toString());
			return ensureViaKeytool(dataDir, cnHint);
		}
	}

	private static SSLContext ensureViaKeytool(Path dataDir, String cnHint) {
		try {
			Path ksPath = dataDir.resolve(KS_NAME);
			Path pwPath = dataDir.resolve(PASS_NAME);
			String password = Files.exists(pwPath)
					? Files.readString(pwPath, StandardCharsets.UTF_8).trim()
					: randomPassword();
			writePassword(pwPath, password);

			String javaHome = System.getProperty("java.home");
			Path keytool = Paths.get(javaHome, "bin", "keytool");
			if (!Files.exists(keytool)) {
				throw new IllegalStateException("keytool not found at " + keytool);
			}
			String dname = "CN=" + ((cnHint != null && !cnHint.isBlank()) ? cnHint : "localhost");

			Process p = new ProcessBuilder(
					keytool.toString(),
					"-genkeypair",
					"-alias", ALIAS,
					"-keyalg", "RSA",
					"-keysize", "2048",
					"-storetype", STORE_TYPE,
					"-validity", "3650",
					"-keystore", ksPath.toString(),
					"-storepass", password,
					"-keypass", password,
					"-dname", dname).redirectErrorStream(true).start();
			int rc = p.waitFor();
			if (rc != 0)
				throw new IllegalStateException("keytool exited with " + rc);

			KeyStore ks = KeyStore.getInstance(STORE_TYPE);
			try (InputStream in = Files.newInputStream(ksPath)) {
				ks.load(in, password.toCharArray());
			}
			KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			kmf.init(ks, password.toCharArray());
			SSLContext ssl = SSLContext.getInstance("TLS");
			ssl.init(kmf.getKeyManagers(), null, null);

			try {
				String spki = spkiPinFromKeystore(ks, password);
				if (spki != null) {
					Log.success(true, "TLS SPKI pin: {}", spki);
				}
			} catch (Exception e) {
				Log.warn("Could not compute SPKI pin (keytool path): {}", e.toString());
			}

			return ssl;
		} catch (Exception e) {
			throw new IllegalStateException("TLS bootstrap failed", e);
		}
	}

	public static javax.net.ssl.SSLContext fromKeystore(String path, String type, String password) {
		try {
			if (path == null || path.isBlank())
				throw new IllegalArgumentException("keystore path is empty");
			if (password == null)
				throw new IllegalArgumentException("keystore password is null");
			String t = (type == null || type.isBlank()) ? "PKCS12" : type;

			java.security.KeyStore ks = java.security.KeyStore.getInstance(t);
			try (java.io.InputStream in = java.nio.file.Files.newInputStream(java.nio.file.Path.of(path))) {
				ks.load(in, password.toCharArray());
			}

			javax.net.ssl.KeyManagerFactory kmf = javax.net.ssl.KeyManagerFactory.getInstance(
					javax.net.ssl.KeyManagerFactory.getDefaultAlgorithm());
			kmf.init(ks, password.toCharArray());

			javax.net.ssl.SSLContext ssl = javax.net.ssl.SSLContext.getInstance("TLS");
			ssl.init(kmf.getKeyManagers(), null, null);

			// Best-effort: log SPKI like TOFU path does
			try {
				var aliases = ks.aliases();
				while (aliases.hasMoreElements()) {
					String alias = aliases.nextElement();
					if (ks.isKeyEntry(alias)) {
						var cert = (java.security.cert.X509Certificate) ks
								.getCertificate(alias);
						String spki = "sha256/" + java.util.Base64.getEncoder().encodeToString(
								java.security.MessageDigest.getInstance("SHA-256")
										.digest(cert.getPublicKey()
												.getEncoded()));
						Log.success(true, "TLS SPKI pin: {}", spki);
						break;
					}
				}
			} catch (Exception ignore) {
			}
			return ssl;
		} catch (Exception e) {
			throw new IllegalStateException("Failed to load STRICT keystore", e);
		}
	}

	private static String spkiPinFromKeystore(KeyStore ks, String password) throws Exception {
		Certificate c = ks.getCertificate(ALIAS);
		if (c instanceof X509Certificate x) {
			return spkiPin(x);
		}
		return null;
	}

	public static String readServerSpkiPin(Path dataDir) {
		try {
			Path ksPath = dataDir.resolve(KS_NAME);
			Path pwPath = dataDir.resolve(PASS_NAME);
			if (!Files.exists(ksPath) || !Files.exists(pwPath))
				return null;
			String password = Files.readString(pwPath, StandardCharsets.UTF_8).trim();
			KeyStore ks = KeyStore.getInstance(STORE_TYPE);
			try (InputStream in = Files.newInputStream(ksPath)) {
				ks.load(in, password.toCharArray());
			}
			return spkiPinFromKeystore(ks, password);
		} catch (Exception e) {
			return null;
		}
	}

	private static void ensureBcProvider() {
		Provider bc = Security.getProvider("BC");
		if (bc == null) {
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	private static void writePassword(Path pwPath, String password) throws Exception {
		Files.writeString(pwPath, password + System.lineSeparator(),
				StandardCharsets.UTF_8, StandardOpenOption.CREATE,
				StandardOpenOption.TRUNCATE_EXISTING);
		try {
			Files.setPosixFilePermissions(pwPath,
					Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
		} catch (UnsupportedOperationException ignored) {
		}
	}

	private static String randomPassword() {
		byte[] raw = new byte[24];
		new SecureRandom().nextBytes(raw);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
	}

	private static void generateKeystoreBc(Path ksPath, String password, String cn) throws Exception {
		KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
		kpg.initialize(2048, new SecureRandom());
		KeyPair kp = kpg.generateKeyPair();

		ZonedDateTime notBefore = ZonedDateTime.now().minusMinutes(1);
		ZonedDateTime notAfter = notBefore.plus(3650, ChronoUnit.DAYS);

		X500Name subject = new X500Name("CN=" + cn);
		BigInteger serial = new BigInteger(64, new SecureRandom());

		SubjectPublicKeyInfo spki = SubjectPublicKeyInfo.getInstance(kp.getPublic().getEncoded());
		X509v3CertificateBuilder b = new X509v3CertificateBuilder(
				subject, serial, Date.from(notBefore.toInstant()), Date.from(notAfter.toInstant()),
				subject, spki);

		JcaX509ExtensionUtils ext = new JcaX509ExtensionUtils();
		b.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
		b.addExtension(Extension.subjectKeyIdentifier, false, ext.createSubjectKeyIdentifier(kp.getPublic()));
		b.addExtension(Extension.keyUsage, true,
				new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment));
		b.addExtension(Extension.extendedKeyUsage, false, new ExtendedKeyUsage(new KeyPurposeId[] {
				KeyPurposeId.id_kp_serverAuth
		}));
		b.addExtension(Extension.subjectAlternativeName, false, new GeneralNames(
				new GeneralName(GeneralName.dNSName, cn)));

		ContentSigner signer = new JcaContentSignerBuilder(SIG_ALG).build(kp.getPrivate());
		X509CertificateHolder holder = b.build(signer);
		X509Certificate cert = new JcaX509CertificateConverter().getCertificate(holder);

		KeyStore ks = KeyStore.getInstance(STORE_TYPE);
		ks.load(null, password.toCharArray());
		ks.setKeyEntry(ALIAS, kp.getPrivate(), password.toCharArray(), new Certificate[] { cert });
		try (OutputStream out = Files.newOutputStream(ksPath, StandardOpenOption.CREATE,
				StandardOpenOption.TRUNCATE_EXISTING)) {
			ks.store(out, password.toCharArray());
		}
	}

	private static String spkiPin(X509Certificate cert) throws Exception {
		byte[] spki = cert.getPublicKey().getEncoded();
		byte[] sha = MessageDigest.getInstance("SHA-256").digest(spki);
		return "sha256/" + Base64.getEncoder().encodeToString(sha);
	}
}
