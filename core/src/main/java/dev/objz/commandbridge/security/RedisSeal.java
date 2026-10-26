package dev.objz.commandbridge.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.function.LongSupplier;

/** Authenticates Redis messages and their destination, with a bounded replay window. */
public final class RedisSeal {
    // Allow clock skew between the proxy and backends.
    static final long WINDOW_MS = 120_000;
    static final int MAX_MESSAGE_CHARS = 1_048_576;
    static final int MAX_SEEN = 65_536;
    private static final byte[] SALT = "commandbridge-redis-v1".getBytes(StandardCharsets.UTF_8);
    private static final byte[] INFO = "hmac-sha256".getBytes(StandardCharsets.UTF_8);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SecretKeySpec key;
    private final LongSupplier clock;
    private final Map<String, Long> seen = new HashMap<>();
    private long nextCleanup;

    public RedisSeal(String secret) {
        this(secret, System::currentTimeMillis);
    }

    RedisSeal(String secret, LongSupplier clock) {
        try {
            this.key = new SecretKeySpec(PairCrypto.hkdf32(secret.getBytes(StandardCharsets.UTF_8), SALT, INFO), "HmacSHA256");
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
        this.clock = clock;
    }

    public String seal(String channel, String message) {
        long t = clock.getAsLong();
        byte[] nonce = new byte[16];
        RANDOM.nextBytes(nonce);
        String n = HexFormat.of().formatHex(nonce);
        ObjectNode out = MAPPER.createObjectNode();
        out.put("t", t);
        out.put("n", n);
        out.put("m", message);
        out.put("s", sign(channel, t, n, message));
        String sealed = out.toString();
        if (sealed.length() > MAX_MESSAGE_CHARS) {
            throw new IllegalArgumentException("Redis message exceeds the size limit");
        }
        return sealed;
    }

    /** Returns null for invalid, expired, replayed, or misrouted messages. */
    public synchronized String open(String channel, String text) {
        if (text == null || text.length() > MAX_MESSAGE_CHARS) {
            return null;
        }
        JsonNode in;
        try {
            in = MAPPER.readTree(text);
        } catch (Exception e) {
            return null;
        }
        if (in == null || !in.path("t").isIntegralNumber() || !in.path("t").canConvertToLong()
                || !in.path("n").isTextual() || !in.path("m").isTextual()
                || !in.path("s").isTextual())
            return null;
        long t = in.path("t").asLong();
        String n = in.path("n").asText();
        String m = in.path("m").asText();
        long now = clock.getAsLong();
        if (t < now - WINDOW_MS || t > now + WINDOW_MS || !n.matches("[0-9a-f]{32}")
                || !in.path("s").asText().matches("[A-Za-z0-9_-]{43}"))
            return null;
        final String expected;
        try {
            expected = sign(channel, t, n, m);
        } catch (IllegalArgumentException e) {
            return null;
        }
        if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII),
                in.path("s").asText().getBytes(StandardCharsets.US_ASCII)))
            return null;
        if (now >= nextCleanup) {
            seen.values().removeIf(at -> at < now - WINDOW_MS);
            nextCleanup = now + 1000;
        }
        // Refuse new messages rather than evicting live nonces and permitting replays.
        if (seen.size() >= MAX_SEEN || seen.putIfAbsent(n, t) != null)
            return null;
        return m;
    }

    private String sign(String channel, long t, String n, String m) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(key);
            // String.getBytes replaces malformed surrogates, which would allow signed payloads to change.
            mac.update(StandardCharsets.UTF_8.newEncoder().encode(
                    CharBuffer.wrap(channel.length() + ":" + channel + "\n" + t + "\n" + n + "\n" + m)));
            byte[] out = mac.doFinal();
            return Base64.getUrlEncoder().withoutPadding().encodeToString(out);
        } catch (CharacterCodingException e) {
            throw new IllegalArgumentException("Redis messages must contain valid Unicode", e);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
