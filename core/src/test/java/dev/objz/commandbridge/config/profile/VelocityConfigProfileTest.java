package dev.objz.commandbridge.config.profile;

import dev.objz.commandbridge.TestFixtures;
import dev.objz.commandbridge.config.model.EndpointType;
import dev.objz.commandbridge.config.model.TlsMode;
import dev.objz.commandbridge.config.model.VelocityConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class VelocityConfigProfileTest {

    private static final VelocityConfigProfile PROFILE = new VelocityConfigProfile();
    private static final VelocityConfig D = VelocityConfig.defaults();

    @BeforeAll
    static void setUp() {
        TestFixtures.ensureLog();
    }

    private static VelocityConfig with(String serverId, EndpointType type, VelocityConfig.Endpoints endpoints,
            VelocityConfig.Security security, VelocityConfig.Timeouts timeouts, VelocityConfig.Tasks tasks) {
        return new VelocityConfig(false, serverId, type, endpoints, security, timeouts, tasks, false);
    }

    private static VelocityConfig withServerId(String serverId) {
        return with(serverId, D.endpointType(), D.endpoints(), D.security(), D.timeouts(), D.tasks());
    }

    private static VelocityConfig withEndpoints(String bindHost, int bindPort, String redisHost, int redisPort) {
        return with(D.serverId(), D.endpointType(), new VelocityConfig.Endpoints(
                new VelocityConfig.Endpoints.WebSocket(bindHost, bindPort),
                new VelocityConfig.Endpoints.Redis(redisHost, redisPort, "", "")),
                D.security(), D.timeouts(), D.tasks());
    }

    private static VelocityConfig withSecurity(EndpointType type, VelocityConfig.Security security) {
        return with(D.serverId(), type, D.endpoints(), security, D.timeouts(), D.tasks());
    }

    private static VelocityConfig withTimeouts(int register, int ping) {
        return with(D.serverId(), D.endpointType(), D.endpoints(), D.security(),
                new VelocityConfig.Timeouts(register, ping), D.tasks());
    }

    private static VelocityConfig withExpireAfter(int expireAfter) {
        return with(D.serverId(), D.endpointType(), D.endpoints(), D.security(), D.timeouts(),
                new VelocityConfig.Tasks(expireAfter));
    }

    @Test
    void defaultsAreValid() {
        var result = PROFILE.normalize(D);
        assertTrue(result.ok());
        assertEquals(D.endpoints(), result.config().endpoints());
        assertEquals(D.timeouts(), result.config().timeouts());
        assertEquals(D.security().tlsMode(), result.config().security().tlsMode());
    }

    @Test
    void blankServerIdFallsBackToDefaultWithoutFailing() {
        var result = PROFILE.normalize(withServerId("  "));
        assertTrue(result.ok());
        assertEquals("proxy-1", result.config().serverId());
    }

    @Test
    void serverIdIsTrimmed() {
        assertEquals("proxy-2", PROFILE.normalize(withServerId(" proxy-2 ")).config().serverId());
    }

    @Test
    void nullEndpointTypeIsInvalidAndFallsBackToWebsocket() {
        var result = PROFILE.normalize(with(D.serverId(), null, D.endpoints(), D.security(), D.timeouts(), D.tasks()));
        assertFalse(result.ok());
        assertEquals(EndpointType.WEBSOCKET, result.config().endpointType());
    }

    @Test
    void websocketSchemeIsStrippedFromBindHost() {
        var result = PROFILE.normalize(withEndpoints("wss://0.0.0.0", 8765, "127.0.0.1", 6379));
        assertTrue(result.ok());
        assertEquals("0.0.0.0", result.config().endpoints().webSocket().bindHost());
    }

    @Test
    void redisSchemeIsStrippedFromHost() {
        var result = PROFILE.normalize(withEndpoints("0.0.0.0", 8765, "rediss://h", 6379));
        assertTrue(result.ok());
        assertEquals("h", result.config().endpoints().redis().host());
    }

    @Test
    void blankBindHostIsInvalid() {
        var result = PROFILE.normalize(withEndpoints(" ", 8765, "127.0.0.1", 6379));
        assertFalse(result.ok());
        assertEquals("0.0.0.0", result.config().endpoints().webSocket().bindHost());
    }

    @Test
    void bindPortZeroFallsBackToDefaultAndIsInvalid() {
        var result = PROFILE.normalize(withEndpoints("0.0.0.0", 0, "127.0.0.1", 6379));
        assertFalse(result.ok());
        assertEquals(8765, result.config().endpoints().webSocket().bindPort());
    }

    @Test
    void bindPortAboveRangeFallsBackToDefaultAndIsInvalid() {
        var result = PROFILE.normalize(withEndpoints("0.0.0.0", 65536, "127.0.0.1", 6379));
        assertFalse(result.ok());
        assertEquals(8765, result.config().endpoints().webSocket().bindPort());
    }

    @Test
    void redisPortAboveRangeFallsBackToDefaultAndIsInvalid() {
        var result = PROFILE.normalize(withEndpoints("0.0.0.0", 8765, "127.0.0.1", 65536));
        assertFalse(result.ok());
        assertEquals(6379, result.config().endpoints().redis().port());
    }

    @Test
    void highestPortIsAccepted() {
        var result = PROFILE.normalize(withEndpoints("0.0.0.0", 65535, "127.0.0.1", 65535));
        assertTrue(result.ok());
        assertEquals(65535, result.config().endpoints().webSocket().bindPort());
        assertEquals(65535, result.config().endpoints().redis().port());
    }

    @Test
    void zeroRegisterTimeoutIsInvalid() {
        var result = PROFILE.normalize(withTimeouts(0, 5));
        assertFalse(result.ok());
        assertEquals(5, result.config().timeouts().registerTimeout());
    }

    @Test
    void negativePingTimeoutIsInvalid() {
        var result = PROFILE.normalize(withTimeouts(5, -1));
        assertFalse(result.ok());
        assertEquals(5, result.config().timeouts().pingTimeout());
    }

    @Test
    void negativeExpireAfterIsInvalid() {
        var result = PROFILE.normalize(withExpireAfter(-1));
        assertFalse(result.ok());
        assertEquals(86400, result.config().tasks().expireAfter());
    }

    @Test
    void zeroExpireAfterDisablesExpiryAndIsValid() {
        var result = PROFILE.normalize(withExpireAfter(0));
        assertTrue(result.ok());
        assertEquals(0, result.config().tasks().expireAfter());
    }

    @Test
    void strictWebsocketWithoutKeystorePathIsInvalid() {
        var result = PROFILE.normalize(withSecurity(EndpointType.WEBSOCKET,
                new VelocityConfig.Security(TlsMode.STRICT, "", "secret", "PKCS12")));
        assertFalse(result.ok());
    }

    @Test
    void strictWebsocketWithoutKeystorePasswordIsInvalid() {
        var result = PROFILE.normalize(withSecurity(EndpointType.WEBSOCKET,
                new VelocityConfig.Security(TlsMode.STRICT, "/keys/ks.p12", " ", "PKCS12")));
        assertFalse(result.ok());
    }

    @Test
    void strictWebsocketWithKeystoreIsValid() {
        var result = PROFILE.normalize(withSecurity(EndpointType.WEBSOCKET,
                new VelocityConfig.Security(TlsMode.STRICT, " /keys/ks.p12 ", "secret", "JKS")));
        assertTrue(result.ok());
        assertEquals("/keys/ks.p12", result.config().security().keystorePath());
        assertEquals("JKS", result.config().security().keystoreType());
    }

    @Test
    void strictWithRedisDoesNotNeedKeystore() {
        var result = PROFILE.normalize(withSecurity(EndpointType.REDIS,
                new VelocityConfig.Security(TlsMode.STRICT, "", "", "PKCS12")));
        assertTrue(result.ok());
    }

    @Test
    void nullTlsModeIsInvalidAndFallsBackToTofu() {
        var result = PROFILE.normalize(withSecurity(EndpointType.WEBSOCKET,
                new VelocityConfig.Security(null, "", "", "PKCS12")));
        assertFalse(result.ok());
        assertEquals(TlsMode.TOFU, result.config().security().tlsMode());
    }

    @Test
    void blankKeystoreTypeFallsBackToDefault() {
        var result = PROFILE.normalize(withSecurity(EndpointType.WEBSOCKET,
                new VelocityConfig.Security(TlsMode.TOFU, "", "", " ")));
        assertTrue(result.ok());
        assertEquals("PKCS12", result.config().security().keystoreType());
    }

    @Test
    void nullSectionsFallBackToDefaultsWithoutNpe() {
        var result = PROFILE.normalize(new VelocityConfig(true, null, EndpointType.REDIS, null, null, null, null, true));
        assertTrue(result.ok());
        VelocityConfig out = result.config();
        assertEquals(D.endpoints(), out.endpoints());
        assertEquals(D.timeouts(), out.timeouts());
        assertEquals(D.tasks(), out.tasks());
        assertEquals(D.security().tlsMode(), out.security().tlsMode());
        assertEquals(D.security().keystoreType(), out.security().keystoreType());
        assertTrue(out.actAsClient());
        assertTrue(out.debug());
    }

    @Test
    void nullNestedEndpointSectionsFallBackToDefaults() {
        var result = PROFILE.normalize(with(D.serverId(), D.endpointType(),
                new VelocityConfig.Endpoints(null, null), D.security(), D.timeouts(), D.tasks()));
        assertTrue(result.ok());
        assertEquals(D.endpoints(), result.config().endpoints());
    }

    @Test
    void nullRedisCredentialsBecomeEmpty() {
        var result = PROFILE.normalize(with(D.serverId(), D.endpointType(), new VelocityConfig.Endpoints(
                D.endpoints().webSocket(), new VelocityConfig.Endpoints.Redis("h", 6379, null, null)),
                D.security(), D.timeouts(), D.tasks()));
        assertEquals("", result.config().endpoints().redis().username());
        assertEquals("", result.config().endpoints().redis().password());
    }
}
