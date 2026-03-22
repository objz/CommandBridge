package dev.objz.commandbridge.scripting.bind.adapters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import dev.objz.commandbridge.scripting.bind.BindContext;
import dev.objz.commandbridge.scripting.bind.ConvertContext;
import dev.objz.commandbridge.scripting.model.enums.Location;
import dev.objz.commandbridge.scripting.platform.PlatformFeatures;
import dev.objz.commandbridge.scripting.validation.ProblemSink;
import dev.objz.commandbridge.scripting.yaml.YamlNode;

class EnumAdapterTest {

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
    void parsesExactMatch() {
        EnumAdapter adapter = new EnumAdapter();
        YamlNode node = YamlNode.scalar("BACKEND");
        Enum<?> result = adapter.fromYaml(node, Location.class, ctx());
        assertEquals(Location.BACKEND, result);
    }

    @Test
    void parsesLowercase() {
        EnumAdapter adapter = new EnumAdapter();
        YamlNode node = YamlNode.scalar("backend");
        Enum<?> result = adapter.fromYaml(node, Location.class, ctx());
        assertEquals(Location.BACKEND, result);
    }

    @Test
    void parsesUppercase() {
        EnumAdapter adapter = new EnumAdapter();
        YamlNode node = YamlNode.scalar("VELOCITY");
        Enum<?> result = adapter.fromYaml(node, Location.class, ctx());
        assertEquals(Location.VELOCITY, result);
    }

    @Test
    void parsesMixedCase() {
        EnumAdapter adapter = new EnumAdapter();
        YamlNode node = YamlNode.scalar("VeLoCiTy");
        Enum<?> result = adapter.fromYaml(node, Location.class, ctx());
        assertEquals(Location.VELOCITY, result);
    }

    @Test
    void returnsNullForNullValue() {
        EnumAdapter adapter = new EnumAdapter();
        YamlNode node = YamlNode.scalar(null);
        Enum<?> result = adapter.fromYaml(node, Location.class, ctx());
        assertNull(result);
    }

    @Test
    void throwsForNonScalarNode() {
        EnumAdapter adapter = new EnumAdapter();
        YamlNode node = YamlNode.mapping(java.util.Map.of());
        assertThrows(IllegalArgumentException.class, () -> adapter.fromYaml(node, Location.class, ctx()));
    }

    @Test
    void throwsForUnknownEnumValue() {
        EnumAdapter adapter = new EnumAdapter();
        YamlNode node = YamlNode.scalar("UNKNOWN");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> adapter.fromYaml(node, Location.class, ctx()));
        assertEquals("Unknown enum constant: UNKNOWN for Location", ex.getMessage());
    }

    @Test
    void throwsForNonEnumTargetType() {
        EnumAdapter adapter = new EnumAdapter();
        YamlNode node = YamlNode.scalar("BACKEND");
        assertThrows(IllegalArgumentException.class, () -> adapter.fromYaml(node, String.class, ctx()));
    }

    @Test
    void convertsToYamlName() {
        EnumAdapter adapter = new EnumAdapter();
        YamlNode result = adapter.toYaml(Location.BACKEND, Location.class, ctx());
        assertEquals(YamlNode.scalar("BACKEND"), result);
    }

    @Test
    void convertsNullToYamlNull() {
        EnumAdapter adapter = new EnumAdapter();
        YamlNode result = adapter.toYaml(null, Location.class, ctx());
        assertEquals(YamlNode.scalar(null), result);
    }

    @Test
    void supportsEnumTypes() {
        EnumAdapter adapter = new EnumAdapter();
        assertEquals(true, adapter.supports(Location.class));
    }

    @Test
    void doesNotSupportNonEnumTypes() {
        EnumAdapter adapter = new EnumAdapter();
        assertEquals(false, adapter.supports(String.class));
        assertEquals(false, adapter.supports(Integer.class));
    }
}
