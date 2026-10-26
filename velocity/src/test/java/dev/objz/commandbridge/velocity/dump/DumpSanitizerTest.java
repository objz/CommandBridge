package dev.objz.commandbridge.velocity.dump;

import com.fasterxml.jackson.databind.JsonNode;
import dev.objz.commandbridge.net.proto.Envelope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DumpSanitizerTest {

    private static final String REDACTED = "<redacted>";

    private static JsonNode sanitize(String json) throws Exception {
        return DumpSanitizer.sanitize(Envelope.MAPPER.readTree(json));
    }

    @Test
    void secretsInNestedObjectsAreRedacted() throws Exception {
        JsonNode out = sanitize("""
                {"config": {
                   "server-id": "proxy-1",
                   "security": {"secret": "s3cret", "tls-pin": "sha256/abc", "keystore-password": "pw"},
                   "endpoints": {"redis": {"host": "h", "redis-password": "pw", "password": "pw"}},
                   "api": {"token": "t", "Authorization": "Bearer t", "api-key": "k"}
                }}
                """);

        JsonNode config = out.get("config");
        assertEquals("proxy-1", config.get("server-id").asText());
        assertEquals(REDACTED, config.at("/security/secret").asText());
        assertEquals(REDACTED, config.at("/security/tls-pin").asText());
        assertEquals(REDACTED, config.at("/security/keystore-password").asText());
        assertEquals(REDACTED, config.at("/endpoints/redis/redis-password").asText());
        assertEquals(REDACTED, config.at("/endpoints/redis/password").asText());
        assertEquals("h", config.at("/endpoints/redis/host").asText());
        assertEquals(REDACTED, config.at("/api/token").asText());
        assertEquals(REDACTED, config.at("/api/Authorization").asText());
        assertEquals(REDACTED, config.at("/api/api-key").asText());
    }

    @Test
    void sensitiveObjectValueIsReplacedWhole() throws Exception {
        JsonNode out = sanitize("{\"secret\": {\"nested\": \"value\"}}");

        assertEquals(REDACTED, out.get("secret").asText());
    }

    @Test
    void arraysAreWalked() throws Exception {
        JsonNode out = sanitize("""
                {"clients": [{"id": "a", "secret": "x"}, {"id": "b", "auth-token": "y"}, "plain"]}
                """);

        assertEquals("a", out.at("/clients/0/id").asText());
        assertEquals(REDACTED, out.at("/clients/0/secret").asText());
        assertEquals(REDACTED, out.at("/clients/1/auth-token").asText());
        assertEquals("plain", out.at("/clients/2").asText());
    }

    @Test
    void secretKeyIsRedacted() throws Exception {
        JsonNode out = sanitize("{\"security\": {\"secret-key\": \"s3cret\", \"shared_secret\": \"s3cret\"}}");

        assertEquals(REDACTED, out.at("/security/secret-key").asText());
        assertEquals(REDACTED, out.at("/security/shared_secret").asText());
    }

    @Test
    void nullAndScalarsArePreserved() throws Exception {
        assertTrue(DumpSanitizer.sanitize(null).isNull());
        JsonNode out = sanitize("{\"count\": 3, \"debug\": true, \"missing\": null}");

        assertEquals(3, out.get("count").asInt());
        assertTrue(out.get("debug").asBoolean());
        assertTrue(out.get("missing").isNull());
    }

    @Test
    void inputIsNotModified() throws Exception {
        JsonNode in = Envelope.MAPPER.readTree("{\"secret\": \"s3cret\"}");

        DumpSanitizer.sanitize(in);

        assertEquals("s3cret", in.get("secret").asText());
    }
}
