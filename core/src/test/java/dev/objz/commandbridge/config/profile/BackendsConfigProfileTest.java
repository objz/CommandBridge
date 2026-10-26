package dev.objz.commandbridge.config.profile;

import dev.objz.commandbridge.TestFixtures;
import dev.objz.commandbridge.config.model.BackendsConfig;
import dev.objz.commandbridge.config.model.EndpointType;
import dev.objz.commandbridge.config.model.TlsMode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BackendsConfigProfileTest {

    private static final BackendsConfigProfile PROFILE = new BackendsConfigProfile();
    private static final BackendsConfig D = BackendsConfig.defaults();

    @BeforeAll
    static void setUp() {
        TestFixtures.ensureLog();
    }

    private static BackendsConfig with(String clientId, EndpointType type, BackendsConfig.Endpoints endpoints,
            BackendsConfig.Security security, BackendsConfig.Timeouts timeouts) {
        return new BackendsConfig(clientId, type, endpoints, security, timeouts, false);
    }

    private static BackendsConfig withEndpoints(String wsHost, int wsPort, String redisHost, int redisPort) {
        return with(D.clientId(), D.endpointType(), new BackendsConfig.Endpoints(
                new BackendsConfig.Endpoints.WebSocket(wsHost, wsPort),
                new BackendsConfig.Endpoints.Redis(redisHost, redisPort, "", "")),
                D.security(), D.timeouts());
    }

    private static BackendsConfig withSecurity(BackendsConfig.Security security) {
        return with(D.clientId(), D.endpointType(), D.endpoints(), security, D.timeouts());
    }

    private static BackendsConfig withTimeouts(int auth, int reconnectTimeout, int reconnectInterval) {
        return with(D.clientId(), D.endpointType(), D.endpoints(), D.security(),
                new BackendsConfig.Timeouts(auth, reconnectTimeout, reconnectInterval));
    }

    @Test
    void defaultsAreValidAndUnchanged() {
        var result = PROFILE.normalize(D);
        assertTrue(result.ok());
        assertEquals(D, result.config());
    }

    @Test
    void blankClientIdIsInvalidAndFallsBackToDefault() {
        var result = PROFILE.normalize(with(" ", D.endpointType(), D.endpoints(), D.security(), D.timeouts()));
        assertFalse(result.ok());
        assertEquals("survival-1", result.config().clientId());
    }

    @Test
    void nullEndpointTypeIsInvalid() {
        var result = PROFILE.normalize(with(D.clientId(), null, D.endpoints(), D.security(), D.timeouts()));
        assertFalse(result.ok());
        assertEquals(EndpointType.WEBSOCKET, result.config().endpointType());
    }

    @Test
    void websocketSchemeIsStrippedFromHost() {
        var result = PROFILE.normalize(withEndpoints("wss://0.0.0.0", 8765, "127.0.0.1", 6379));
        assertTrue(result.ok());
        assertEquals("0.0.0.0", result.config().endpoints().websocket().host());
    }

    @Test
    void redisSchemeIsStrippedFromHost() {
        var result = PROFILE.normalize(withEndpoints("127.0.0.1", 8765, "rediss://h", 6379));
        assertTrue(result.ok());
        assertEquals("h", result.config().endpoints().redis().host());
    }

    @Test
    void portZeroFallsBackToDefaultAndIsInvalid() {
        var result = PROFILE.normalize(withEndpoints("127.0.0.1", 0, "127.0.0.1", 6379));
        assertFalse(result.ok());
        assertEquals(8765, result.config().endpoints().websocket().port());
    }

    @Test
    void portAboveRangeFallsBackToDefaultAndIsInvalid() {
        var result = PROFILE.normalize(withEndpoints("127.0.0.1", 8765, "127.0.0.1", 65536));
        assertFalse(result.ok());
        assertEquals(6379, result.config().endpoints().redis().port());
    }

    @Test
    void highestPortIsAccepted() {
        var result = PROFILE.normalize(withEndpoints("127.0.0.1", 65535, "127.0.0.1", 65535));
        assertTrue(result.ok());
        assertEquals(65535, result.config().endpoints().websocket().port());
    }

    @Test
    void zeroAuthTimeoutIsInvalid() {
        var result = PROFILE.normalize(withTimeouts(0, 60, 5));
        assertFalse(result.ok());
        assertEquals(5, result.config().timeouts().authTimeout());
    }

    @Test
    void negativeReconnectTimeoutIsInvalid() {
        var result = PROFILE.normalize(withTimeouts(5, -3, 5));
        assertFalse(result.ok());
        assertEquals(60, result.config().timeouts().reconnectTimeout());
    }

    @Test
    void zeroReconnectIntervalIsInvalid() {
        var result = PROFILE.normalize(withTimeouts(5, 60, 0));
        assertFalse(result.ok());
        assertEquals(5, result.config().timeouts().reconnectInterval());
    }

    @Test
    void blankSecretIsInvalid() {
        assertFalse(PROFILE.normalize(withSecurity(new BackendsConfig.Security(TlsMode.TOFU, "", "  "))).ok());
    }

    @Test
    void placeholderSecretOnlyWarns() {
        var result = PROFILE.normalize(withSecurity(new BackendsConfig.Security(TlsMode.TOFU, "", "change-me")));
        assertTrue(result.ok());
        assertEquals("change-me", result.config().security().secret());
    }

    @Test
    void nullTlsModeIsInvalidAndFallsBackToTofu() {
        var result = PROFILE.normalize(withSecurity(new BackendsConfig.Security(null, "", "s3cret")));
        assertFalse(result.ok());
        assertEquals(TlsMode.TOFU, result.config().security().tlsMode());
    }

    @Test
    void tlsPinIsTrimmed() {
        var result = PROFILE.normalize(withSecurity(new BackendsConfig.Security(TlsMode.STRICT, " sha256/abc ", "s3cret")));
        assertTrue(result.ok());
        assertEquals("sha256/abc", result.config().security().tlsPin());
    }

    @Test
    void nullSectionsFallBackToDefaultsWithoutNpe() {
        var result = PROFILE.normalize(new BackendsConfig("lobby", EndpointType.REDIS, null, null, null, true));
        assertTrue(result.ok());
        BackendsConfig out = result.config();
        assertEquals(D.endpoints(), out.endpoints());
        assertEquals(D.security(), out.security());
        assertEquals(D.timeouts(), out.timeouts());
        assertTrue(out.debug());
    }

    @Test
    void nullNestedEndpointSectionsFallBackToDefaults() {
        var result = PROFILE.normalize(with(D.clientId(), D.endpointType(),
                new BackendsConfig.Endpoints(null, null), D.security(), D.timeouts()));
        assertTrue(result.ok());
        assertEquals(D.endpoints(), result.config().endpoints());
    }
}
