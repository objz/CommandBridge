package dev.objz.commandbridge.scripting.bind.adapters;

import dev.objz.commandbridge.TestFixtures;
import dev.objz.commandbridge.scripting.bind.ConvertContext;
import dev.objz.commandbridge.scripting.bind.TypeAdapterRegistry;
import dev.objz.commandbridge.scripting.validation.ProblemSink;
import dev.objz.commandbridge.scripting.yaml.YamlNode;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link PrimitivesAdapter}.
 * Verifies YAML scalar conversion to Java primitive types with representative type coverage.
 */
final class PrimitivesAdapterTest {

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

    private final PrimitivesAdapter adapter = new PrimitivesAdapter();

    @BeforeAll
    static void setUp() {
        TestFixtures.ensureLog();
    }

    @Test
    void fromYamlValidIntReturnsInteger() {
        Object result = adapter.fromYaml(YamlNode.scalar("42"), int.class, CTX);
        assertEquals(42, result);
    }

    @Test
    void fromYamlValidBooleanReturnsBoolean() {
        Object result = adapter.fromYaml(YamlNode.scalar("true"), boolean.class, CTX);
        assertEquals(true, result);
    }

    @Test
    void fromYamlValidLongReturnsLong() {
        Object result = adapter.fromYaml(YamlNode.scalar("9999999999"), long.class, CTX);
        assertEquals(9999999999L, result);
    }

    @Test
    void fromYamlValidDoubleReturnsDouble() {
        Object result = adapter.fromYaml(YamlNode.scalar("3.14"), double.class, CTX);
        assertEquals(3.14, result);
    }

    @Test
    void fromYamlInvalidNumberThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> adapter.fromYaml(YamlNode.scalar("abc"), int.class, CTX));
    }

    @Test
    void fromYamlNullScalarValueReturnsNull() {
        Object result = adapter.fromYaml(YamlNode.scalar(null), int.class, CTX);
        assertNull(result);
    }

    @Test
    void supportsPrimitiveTypesReturnsTrue() {
        assertTrue(adapter.supports(int.class));
        assertTrue(adapter.supports(boolean.class));
        assertTrue(adapter.supports(long.class));
        assertTrue(adapter.supports(double.class));
    }

    @Test
    void supportsNonPrimitiveTypeReturnsFalse() {
        assertFalse(adapter.supports(String.class));
    }
}
