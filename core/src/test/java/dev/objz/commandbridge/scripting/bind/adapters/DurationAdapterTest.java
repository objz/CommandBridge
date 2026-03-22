package dev.objz.commandbridge.scripting.bind.adapters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import dev.objz.commandbridge.scripting.bind.BindContext;
import dev.objz.commandbridge.scripting.bind.ConvertContext;
import dev.objz.commandbridge.scripting.platform.PlatformFeatures;
import dev.objz.commandbridge.scripting.validation.ProblemSink;
import dev.objz.commandbridge.scripting.yaml.YamlNode;

class DurationAdapterTest {

    @BeforeAll
    static void installLog() {
        try {
            dev.objz.commandbridge.logging.Log.install(java.util.logging.Logger.getLogger("test"));
        } catch (IllegalStateException ignored) {
            // expected
        }
    }

    private static ConvertContext ctx() {
        return new BindContext(null, new ProblemSink(), PlatformFeatures.none());
    }

    @Test
    void parsesMilliseconds() {
        DurationAdapter adapter = new DurationAdapter();
        YamlNode node = YamlNode.scalar("500ms");
        Duration result = adapter.fromYaml(node, Duration.class, ctx());
        assertEquals(Duration.ofMillis(500), result);
    }

    @Test
    void parsesSeconds() {
        DurationAdapter adapter = new DurationAdapter();
        YamlNode node = YamlNode.scalar("30s");
        Duration result = adapter.fromYaml(node, Duration.class, ctx());
        assertEquals(Duration.ofSeconds(30), result);
    }

    @Test
    void parsesMinutes() {
        DurationAdapter adapter = new DurationAdapter();
        YamlNode node = YamlNode.scalar("5m");
        Duration result = adapter.fromYaml(node, Duration.class, ctx());
        assertEquals(Duration.ofMinutes(5), result);
    }

    @Test
    void parsesHours() {
        DurationAdapter adapter = new DurationAdapter();
        YamlNode node = YamlNode.scalar("2h");
        Duration result = adapter.fromYaml(node, Duration.class, ctx());
        assertEquals(Duration.ofHours(2), result);
    }

    @Test
    void parsesPlainNumberAsSeconds() {
        DurationAdapter adapter = new DurationAdapter();
        YamlNode node = YamlNode.scalar("60");
        Duration result = adapter.fromYaml(node, Duration.class, ctx());
        assertEquals(Duration.ofSeconds(60), result);
    }

    @Test
    void parsesNumberValueAsMillis() {
        DurationAdapter adapter = new DurationAdapter();
        YamlNode node = YamlNode.scalar(1000L);
        Duration result = adapter.fromYaml(node, Duration.class, ctx());
        assertEquals(Duration.ofMillis(1000), result);
    }

    @Test
    void parsesIntegerValueAsMillis() {
        DurationAdapter adapter = new DurationAdapter();
        YamlNode node = YamlNode.scalar(500);
        Duration result = adapter.fromYaml(node, Duration.class, ctx());
        assertEquals(Duration.ofMillis(500), result);
    }

    @Test
    void returnsNullForNullValue() {
        DurationAdapter adapter = new DurationAdapter();
        YamlNode node = YamlNode.scalar(null);
        Duration result = adapter.fromYaml(node, Duration.class, ctx());
        assertNull(result);
    }

    @Test
    void throwsForNonScalarNode() {
        DurationAdapter adapter = new DurationAdapter();
        YamlNode node = YamlNode.mapping(java.util.Map.of());
        assertThrows(IllegalArgumentException.class, () -> adapter.fromYaml(node, Duration.class, ctx()));
    }

    @Test
    void throwsForEmptyString() {
        DurationAdapter adapter = new DurationAdapter();
        YamlNode node = YamlNode.scalar("");
        assertThrows(IllegalArgumentException.class, () -> adapter.fromYaml(node, Duration.class, ctx()));
    }

    @Test
    void throwsForInvalidFormat() {
        DurationAdapter adapter = new DurationAdapter();
        YamlNode node = YamlNode.scalar("not-a-duration");
        assertThrows(NumberFormatException.class, () -> adapter.fromYaml(node, Duration.class, ctx()));
    }

    @Test
    void caseInsensitiveUnits() {
        DurationAdapter adapter = new DurationAdapter();
        YamlNode node = YamlNode.scalar("10S");
        Duration result = adapter.fromYaml(node, Duration.class, ctx());
        assertEquals(Duration.ofSeconds(10), result);
    }

    @Test
    void convertsToYamlSeconds() {
        DurationAdapter adapter = new DurationAdapter();
        Duration duration = Duration.ofSeconds(45);
        YamlNode result = adapter.toYaml(duration, Duration.class, ctx());
        assertEquals(YamlNode.scalar("45s"), result);
    }

    @Test
    void convertsToYamlMillis() {
        DurationAdapter adapter = new DurationAdapter();
        Duration duration = Duration.ofMillis(500);
        YamlNode result = adapter.toYaml(duration, Duration.class, ctx());
        assertEquals(YamlNode.scalar("500ms"), result);
    }

    @Test
    void convertsNullToYamlNull() {
        DurationAdapter adapter = new DurationAdapter();
        YamlNode result = adapter.toYaml(null, Duration.class, ctx());
        assertEquals(YamlNode.scalar(null), result);
    }

    @Test
    void supportsOnlyDurationClass() {
        DurationAdapter adapter = new DurationAdapter();
        assertEquals(true, adapter.supports(Duration.class));
        assertEquals(false, adapter.supports(String.class));
        assertEquals(false, adapter.supports(Long.class));
    }
}
