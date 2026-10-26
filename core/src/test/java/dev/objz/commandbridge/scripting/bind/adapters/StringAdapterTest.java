package dev.objz.commandbridge.scripting.bind.adapters;

import dev.objz.commandbridge.scripting.bind.BindContext;
import dev.objz.commandbridge.scripting.bind.TypeAdapterRegistry;
import dev.objz.commandbridge.scripting.platform.PlatformFeatures;
import dev.objz.commandbridge.scripting.validation.ProblemSink;
import dev.objz.commandbridge.scripting.yaml.YamlNode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class StringAdapterTest {

    private static final BindContext CTX = new BindContext(new TypeAdapterRegistry(), new ProblemSink(),
            PlatformFeatures.none());

    private final StringAdapter adapter = new StringAdapter();

    @Test
    void supportsOnlyString() {
        assertTrue(adapter.supports(String.class));
        assertFalse(adapter.supports(Integer.class));
        assertFalse(adapter.supports(CharSequence.class));
    }

    @Test
    void stringScalarIsReturnedAsIs() {
        assertEquals("hello", adapter.fromYaml(YamlNode.scalar("hello"), String.class, CTX));
    }

    @Test
    void nonStringScalarIsStringified() {
        assertEquals("42", adapter.fromYaml(YamlNode.scalar(42), String.class, CTX));
        assertEquals("true", adapter.fromYaml(YamlNode.scalar(true), String.class, CTX));
    }

    @Test
    void nullScalarStaysNull() {
        assertNull(adapter.fromYaml(YamlNode.scalar(null), String.class, CTX));
    }

    @Test
    void nonScalarIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> adapter.fromYaml(YamlNode.sequence(List.of()), String.class, CTX));
        assertThrows(IllegalArgumentException.class,
                () -> adapter.fromYaml(YamlNode.mapping(Map.of()), String.class, CTX));
    }

    @Test
    void toYamlWrapsValueInScalar() {
        assertEquals(YamlNode.scalar("x"), adapter.toYaml("x", String.class, CTX));
    }
}
