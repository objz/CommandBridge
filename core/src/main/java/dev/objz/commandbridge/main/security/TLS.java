package dev.objz.commandbridge.main.security;

import dev.objz.commandbridge.main.logging.Log;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermission;
import java.security.*;
import java.security.cert.X509Certificate;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.Set;

// BouncyCastle
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
				Log.success("Generated self-signed TLS keystore at {}", ksPath);
			}

			KeyStore ks = KeyStore.getInstance(STORE_TYPE);
			try (InputStream in = Files.newInputStream(ksPath)) {
				ks.load(in, password.toCharArray());
			}
			KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			kmf.init(ks, password.toCharArray());

			SSLContext ssl = SSLContext.getInstance("TLS");
			ssl.init(kmf.getKeyManagers(), null, null);
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
			return ssl;
		} catch (Exception e) {
			throw new IllegalStateException("TLS bootstrap failed", e);
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

		BigInteger serial = new BigInteger(160, new SecureRandom()).abs();
		X500Name subject = new X500Name("CN=" + cn);
		X500Name issuer = subject; // self-signed
		SubjectPublicKeyInfo spki = SubjectPublicKeyInfo.getInstance(kp.getPublic().getEncoded());

		X509v3CertificateBuilder builder = new X509v3CertificateBuilder(
				issuer,
				serial,
				Date.from(notBefore.toInstant()),
				Date.from(notAfter.toInstant()),
				subject,
				spki);

		JcaX509ExtensionUtils extUtils = new JcaX509ExtensionUtils();
		builder.addExtension(Extension.subjectKeyIdentifier, false,
				extUtils.createSubjectKeyIdentifier(kp.getPublic()));
		builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
		builder.addExtension(Extension.keyUsage, true,
				new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment));
		builder.addExtension(Extension.extendedKeyUsage, false,
				new ExtendedKeyUsage(KeyPurposeId.id_kp_serverAuth));

		GeneralNames sans = new GeneralNames(new GeneralName[] {
				new GeneralName(GeneralName.dNSName, cn),
				new GeneralName(GeneralName.dNSName, "localhost"),
				new GeneralName(GeneralName.iPAddress, "127.0.0.1"),
				new GeneralName(GeneralName.iPAddress, "::1")
		});
		builder.addExtension(Extension.subjectAlternativeName, false, sans);

		ContentSigner signer = new JcaContentSignerBuilder(SIG_ALG).build(kp.getPrivate());
		X509CertificateHolder holder = builder.build(signer);
		X509Certificate cert = new JcaX509CertificateConverter()
				.setProvider("BC")
				.getCertificate(holder);
		cert.checkValidity(new Date());
		cert.verify(kp.getPublic());

		KeyStore ks = KeyStore.getInstance(STORE_TYPE);
		ks.load(null, null);
		ks.setKeyEntry(ALIAS, kp.getPrivate(), password.toCharArray(),
				new java.security.cert.Certificate[] { cert });
		try (OutputStream out = Files.newOutputStream(ksPath, StandardOpenOption.CREATE_NEW)) {
			ks.store(out, password.toCharArray());
		}
	}
}
