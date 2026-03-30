package dev.objz.commandbridge.scripting.bind.adapters;

import dev.objz.commandbridge.TestFixtures;
import dev.objz.commandbridge.scripting.bind.ConvertContext;
import dev.objz.commandbridge.scripting.bind.TypeAdapterRegistry;
import dev.objz.commandbridge.scripting.validation.ProblemSink;
import dev.objz.commandbridge.scripting.yaml.YamlNode;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link EnumAdapter}.
 * Verifies YAML scalar conversion to Java enum types with case-insensitive matching.
 */
final class EnumAdapterTest {

    private enum TestColor { RED, GREEN, BLUE }

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

    private final EnumAdapter adapter = new EnumAdapter();

    @BeforeAll
    static void setUp() {
        TestFixtures.ensureLog();
    }

    @Test
    void fromYamlExactMatchReturnsEnum() {
        Enum<?> result = adapter.fromYaml(YamlNode.scalar("RED"), TestColor.class, CTX);
        assertEquals(TestColor.RED, result);
    }

    @Test
    void fromYamlLowercaseMatchReturnsEnum() {
        Enum<?> result = adapter.fromYaml(YamlNode.scalar("red"), TestColor.class, CTX);
        assertEquals(TestColor.RED, result);
    }

    @Test
    void fromYamlMixedCaseMatchReturnsEnum() {
        Enum<?> result = adapter.fromYaml(YamlNode.scalar("gReEn"), TestColor.class, CTX);
        assertEquals(TestColor.GREEN, result);
    }

    @Test
    void fromYamlInvalidValueThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> adapter.fromYaml(YamlNode.scalar("PURPLE"), TestColor.class, CTX));
    }

    @Test
    void supportsEnumTypeReturnsTrue() {
        assertTrue(adapter.supports(TestColor.class));
    }
}
