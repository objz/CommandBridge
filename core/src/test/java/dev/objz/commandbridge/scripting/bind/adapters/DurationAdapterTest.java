package dev.objz.commandbridge.scripting.bind.adapters;

import dev.objz.commandbridge.TestFixtures;
import dev.objz.commandbridge.scripting.bind.ConvertContext;
import dev.objz.commandbridge.scripting.bind.TypeAdapterRegistry;
import dev.objz.commandbridge.scripting.validation.ProblemSink;
import dev.objz.commandbridge.scripting.yaml.YamlNode;

import java.time.Duration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for {@link DurationAdapter}.
 * Verifies human-readable duration parsing (30s, 5m, 2h, 500ms) and serialization.
 */
final class DurationAdapterTest {

    private static final ConvertContext CTX = new ConvertContext() {
        @Override
        public TypeAdapterRegistry adapters() {
            return null;
        }

        @Override
        public ProblemSink problems() {
            return new ProblemSink();
        }
    };

    private final DurationAdapter adapter = new DurationAdapter();

    @BeforeAll
    static void setUp() {
        TestFixtures.ensureLog();
    }

    @Test
    void fromYamlSecondsReturnsDuration() {
        Duration result = adapter.fromYaml(YamlNode.scalar("30s"), Duration.class, CTX);
        assertEquals(Duration.ofSeconds(30), result);
    }

    @Test
    void fromYamlMinutesReturnsDuration() {
        Duration result = adapter.fromYaml(YamlNode.scalar("5m"), Duration.class, CTX);
        assertEquals(Duration.ofMinutes(5), result);
    }

    @Test
    void fromYamlHoursReturnsDuration() {
        Duration result = adapter.fromYaml(YamlNode.scalar("2h"), Duration.class, CTX);
        assertEquals(Duration.ofHours(2), result);
    }

    @Test
    void fromYamlMillisecondsReturnsDuration() {
        Duration result = adapter.fromYaml(YamlNode.scalar("500ms"), Duration.class, CTX);
        assertEquals(Duration.ofMillis(500), result);
    }

    @Test
    void fromYamlInvalidFormatThrowsException() {
        assertThrows(NumberFormatException.class,
                () -> adapter.fromYaml(YamlNode.scalar("abc"), Duration.class, CTX));
    }

    @Test
    void fromYamlNullScalarValueReturnsNull() {
        Duration result = adapter.fromYaml(YamlNode.scalar(null), Duration.class, CTX);
        assertNull(result);
    }

    @Test
    void toYamlWholeSecondsReturnsSecondsFormat() {
        YamlNode result = adapter.toYaml(Duration.ofSeconds(30), Duration.class, CTX);
        assertEquals(YamlNode.scalar("30s"), result);
    }

    @Test
    void toYamlSubSecondReturnsMillisFormat() {
        YamlNode result = adapter.toYaml(Duration.ofMillis(1500), Duration.class, CTX);
        assertEquals(YamlNode.scalar("1500ms"), result);
    }
}
