package dev.objz.commandbridge.security;

import dev.objz.commandbridge.config.model.TlsMode;
import dev.objz.commandbridge.config.model.VelocityConfig;
import dev.objz.commandbridge.logging.Log;

import javax.net.ssl.SSLContext;
import java.nio.file.Path;

public final class TlsResolver {
    private TlsResolver() {
    }

    public record ServerTls(boolean enabled, SSLContext context) {
    }

    /**
     * - PLAINTEXT => disabled
     * - TOFU => use self-signed (generated/reused), log SPKI
     * - STRICT => load user keystore, never generates self-signed
     */
    public static ServerTls resolveServer(Path dataDir, VelocityConfig.Security sec) {
        TlsMode mode = sec.tlsMode();
        if (mode == null) {
            Log.warn("security.tls-mode not set; defaulting to TOFU");
            mode = TlsMode.TOFU;
        }

        switch (mode) {
            case PLAIN -> {
                return new ServerTls(false, null);
            }
            case TOFU -> {
                var ssl = TLS.ensure(dataDir.resolve("data"), "localhost");
                return new ServerTls(true, ssl);
            }
            case STRICT -> {
                var ssl = StrictKeystore.load(sec.keystorePath(), sec.keystoreType(),
                        sec.keystorePassword());
                return new ServerTls(true, ssl);
            }
            default -> throw new IllegalStateException("Unhandled tls-mode: " + mode);
        }
    }

    /** Backends convenience */
    public static boolean isTlsEnabled(TlsMode mode) {
        return mode != TlsMode.PLAIN;
    }

    public static String schemeFor(TlsMode mode) {
        return isTlsEnabled(mode) ? "wss" : "ws";
    }
}
