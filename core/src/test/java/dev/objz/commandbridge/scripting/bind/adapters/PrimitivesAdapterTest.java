package dev.objz.commandbridge.scripting.bind.adapters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import dev.objz.commandbridge.scripting.bind.BindContext;
import dev.objz.commandbridge.scripting.bind.TypeAdapterRegistry;
import dev.objz.commandbridge.scripting.yaml.YamlNode;
import dev.objz.commandbridge.scripting.validation.ProblemSink;

class PrimitivesAdapterTest {

    private static PrimitivesAdapter adapter;
    private static BindContext ctx;

    @BeforeAll
    static void installLog() {
        try {
            dev.objz.commandbridge.logging.Log.install(java.util.logging.Logger.getLogger("test"));
        } catch (IllegalStateException ignored) {
            // expected
        }
        adapter = new PrimitivesAdapter();
        ctx = new BindContext(new TypeAdapterRegistry(), new ProblemSink(), null);
    }

    // ===== supports() tests =====

    @Test
    void supportsIntPrimitive() {
        assertTrue(adapter.supports(int.class));
    }

    @Test
    void supportsIntegerWrapper() {
        assertTrue(adapter.supports(Integer.class));
    }

    @Test
    void supportsLongPrimitive() {
        assertTrue(adapter.supports(long.class));
    }

    @Test
    void supportsLongWrapper() {
        assertTrue(adapter.supports(Long.class));
    }

    @Test
    void supportsShortPrimitive() {
        assertTrue(adapter.supports(short.class));
    }

    @Test
    void supportsShortWrapper() {
        assertTrue(adapter.supports(Short.class));
    }

    @Test
    void supportsBytePrimitive() {
        assertTrue(adapter.supports(byte.class));
    }

    @Test
    void supportsbyteWrapper() {
        assertTrue(adapter.supports(Byte.class));
    }

    @Test
    void supportsDoublePrimitive() {
        assertTrue(adapter.supports(double.class));
    }

    @Test
    void supportsDoubleWrapper() {
        assertTrue(adapter.supports(Double.class));
    }

    @Test
    void supportsFloatPrimitive() {
        assertTrue(adapter.supports(float.class));
    }

    @Test
    void supportsFloatWrapper() {
        assertTrue(adapter.supports(Float.class));
    }

    @Test
    void supportsBooleanPrimitive() {
        assertTrue(adapter.supports(boolean.class));
    }

    @Test
    void supportsBooleanWrapper() {
        assertTrue(adapter.supports(Boolean.class));
    }

    @Test
    void supportsCharPrimitive() {
        assertTrue(adapter.supports(char.class));
    }

    @Test
    void supportsCharacterWrapper() {
        assertTrue(adapter.supports(Character.class));
    }

    // ===== fromYaml() - Integer tests =====

    @Test
    void intFromNumber() {
        YamlNode node = YamlNode.scalar(42);
        Object result = adapter.fromYaml(node, int.class, ctx);
        assertEquals(42, result);
    }

    @Test
    void intFromString() {
        YamlNode node = YamlNode.scalar("123");
        Object result = adapter.fromYaml(node, int.class, ctx);
        assertEquals(123, result);
    }

    @Test
    void integerWrapperFromNumber() {
        YamlNode node = YamlNode.scalar(99);
        Object result = adapter.fromYaml(node, Integer.class, ctx);
        assertEquals(99, result);
    }

    @Test
    void intMaxValue() {
        YamlNode node = YamlNode.scalar(Integer.MAX_VALUE);
        Object result = adapter.fromYaml(node, int.class, ctx);
        assertEquals(Integer.MAX_VALUE, result);
    }

    @Test
    void intMinValue() {
        YamlNode node = YamlNode.scalar(Integer.MIN_VALUE);
        Object result = adapter.fromYaml(node, int.class, ctx);
        assertEquals(Integer.MIN_VALUE, result);
    }

    @Test
    void intFromInvalidString() {
        YamlNode node = YamlNode.scalar("not_a_number");
        assertThrows(IllegalArgumentException.class, () -> adapter.fromYaml(node, int.class, ctx));
    }

    // ===== fromYaml() - Long tests =====

    @Test
    void longFromNumber() {
        YamlNode node = YamlNode.scalar(9999999999L);
        Object result = adapter.fromYaml(node, long.class, ctx);
        assertEquals(9999999999L, result);
    }

    @Test
    void longFromString() {
        YamlNode node = YamlNode.scalar("123456789");
        Object result = adapter.fromYaml(node, long.class, ctx);
        assertEquals(123456789L, result);
    }

    @Test
    void longWrapperFromNumber() {
        YamlNode node = YamlNode.scalar(555L);
        Object result = adapter.fromYaml(node, Long.class, ctx);
        assertEquals(555L, result);
    }

    @Test
    void longMaxValue() {
        YamlNode node = YamlNode.scalar(Long.MAX_VALUE);
        Object result = adapter.fromYaml(node, long.class, ctx);
        assertEquals(Long.MAX_VALUE, result);
    }

    // ===== fromYaml() - Double tests =====

    @Test
    void doubleFromNumber() {
        YamlNode node = YamlNode.scalar(3.14);
        Object result = adapter.fromYaml(node, double.class, ctx);
        assertEquals(3.14, result);
    }

    @Test
    void doubleFromString() {
        YamlNode node = YamlNode.scalar("2.71828");
        Object result = adapter.fromYaml(node, double.class, ctx);
        assertEquals(2.71828, (double) result, 0.00001);
    }

    @Test
    void doubleWrapperFromNumber() {
        YamlNode node = YamlNode.scalar(1.5);
        Object result = adapter.fromYaml(node, Double.class, ctx);
        assertEquals(1.5, result);
    }

    @Test
    void doubleFromInvalidString() {
        YamlNode node = YamlNode.scalar("not_a_double");
        assertThrows(IllegalArgumentException.class, () -> adapter.fromYaml(node, double.class, ctx));
    }

    // ===== fromYaml() - Float tests =====

    @Test
    void floatFromNumber() {
        YamlNode node = YamlNode.scalar(1.5f);
        Object result = adapter.fromYaml(node, float.class, ctx);
        assertEquals(1.5f, result);
    }

    @Test
    void floatFromString() {
        YamlNode node = YamlNode.scalar("2.5");
        Object result = adapter.fromYaml(node, float.class, ctx);
        assertEquals(2.5f, (float) result, 0.01f);
    }

    // ===== fromYaml() - Boolean tests =====

    @Test
    void booleanFromBooleanTrue() {
        YamlNode node = YamlNode.scalar(true);
        Object result = adapter.fromYaml(node, boolean.class, ctx);
        assertEquals(true, result);
    }

    @Test
    void booleanFromBooleanFalse() {
        YamlNode node = YamlNode.scalar(false);
        Object result = adapter.fromYaml(node, boolean.class, ctx);
        assertEquals(false, result);
    }

    @Test
    void booleanFromStringTrue() {
        YamlNode node = YamlNode.scalar("true");
        Object result = adapter.fromYaml(node, boolean.class, ctx);
        assertEquals(true, result);
    }

    @Test
    void booleanFromStringFalse() {
        YamlNode node = YamlNode.scalar("false");
        Object result = adapter.fromYaml(node, boolean.class, ctx);
        assertEquals(false, result);
    }

    @Test
    void booleanFromStringYes() {
        YamlNode node = YamlNode.scalar("yes");
        Object result = adapter.fromYaml(node, boolean.class, ctx);
        assertEquals(true, result);
    }

    @Test
    void booleanFromStringNo() {
        YamlNode node = YamlNode.scalar("no");
        Object result = adapter.fromYaml(node, boolean.class, ctx);
        assertEquals(false, result);
    }

    @Test
    void booleanFromStringOne() {
        YamlNode node = YamlNode.scalar("1");
        Object result = adapter.fromYaml(node, boolean.class, ctx);
        assertEquals(true, result);
    }

    @Test
    void booleanFromStringZero() {
        YamlNode node = YamlNode.scalar("0");
        Object result = adapter.fromYaml(node, boolean.class, ctx);
        assertEquals(false, result);
    }

    @Test
    void booleanFromStringOn() {
        YamlNode node = YamlNode.scalar("on");
        Object result = adapter.fromYaml(node, boolean.class, ctx);
        assertEquals(true, result);
    }

    @Test
    void booleanFromStringOff() {
        YamlNode node = YamlNode.scalar("off");
        Object result = adapter.fromYaml(node, boolean.class, ctx);
        assertEquals(false, result);
    }

    @Test
    void booleanFromStringCaseInsensitive() {
        YamlNode node = YamlNode.scalar("TRUE");
        Object result = adapter.fromYaml(node, boolean.class, ctx);
        assertEquals(true, result);
    }

    @Test
    void booleanFromStringWithWhitespace() {
        YamlNode node = YamlNode.scalar("  true  ");
        Object result = adapter.fromYaml(node, boolean.class, ctx);
        assertEquals(true, result);
    }

    @Test
    void booleanFromInvalidString() {
        YamlNode node = YamlNode.scalar("maybe");
        assertThrows(IllegalArgumentException.class, () -> adapter.fromYaml(node, boolean.class, ctx));
    }

    @Test
    void booleanWrapperFromBoolean() {
        YamlNode node = YamlNode.scalar(true);
        Object result = adapter.fromYaml(node, Boolean.class, ctx);
        assertEquals(true, result);
    }

    // ===== fromYaml() - Character tests =====

    @Test
    void charFromSingleCharString() {
        YamlNode node = YamlNode.scalar("A");
        Object result = adapter.fromYaml(node, char.class, ctx);
        assertEquals('A', result);
    }

    @Test
    void charFromDigitString() {
        YamlNode node = YamlNode.scalar("5");
        Object result = adapter.fromYaml(node, char.class, ctx);
        assertEquals('5', result);
    }

    @Test
    void charFromMultiCharStringThrows() {
        YamlNode node = YamlNode.scalar("ABC");
        assertThrows(IllegalArgumentException.class, () -> adapter.fromYaml(node, char.class, ctx));
    }

    @Test
    void characterWrapperFromSingleChar() {
        YamlNode node = YamlNode.scalar("X");
        Object result = adapter.fromYaml(node, Character.class, ctx);
        assertEquals('X', result);
    }

    // ===== fromYaml() - Short tests =====

    @Test
    void shortFromNumber() {
        YamlNode node = YamlNode.scalar((short) 100);
        Object result = adapter.fromYaml(node, short.class, ctx);
        assertEquals((short) 100, result);
    }

    @Test
    void shortFromString() {
        YamlNode node = YamlNode.scalar("999");
        Object result = adapter.fromYaml(node, short.class, ctx);
        assertEquals((short) 999, result);
    }

    // ===== fromYaml() - Byte tests =====

    @Test
    void byteFromNumber() {
        YamlNode node = YamlNode.scalar((byte) 42);
        Object result = adapter.fromYaml(node, byte.class, ctx);
        assertEquals((byte) 42, result);
    }

    @Test
    void byteFromString() {
        YamlNode node = YamlNode.scalar("127");
        Object result = adapter.fromYaml(node, byte.class, ctx);
        assertEquals((byte) 127, result);
    }

    // ===== fromYaml() - null handling =====

    @Test
    void nullNodeReturnsNull() {
        YamlNode node = YamlNode.scalar(null);
        Object result = adapter.fromYaml(node, int.class, ctx);
        assertNull(result);
    }

    @Test
    void nonScalarNodeReturnsNull() {
        YamlNode node = YamlNode.mapping(java.util.Map.of());
        Object result = adapter.fromYaml(node, int.class, ctx);
        assertNull(result);
    }

    // ===== toYaml() tests =====

    @Test
    void toYamlInt() {
        YamlNode result = adapter.toYaml(42, int.class, ctx);
        assertEquals(YamlNode.scalar(42), result);
    }

    @Test
    void toYamlBoolean() {
        YamlNode result = adapter.toYaml(true, boolean.class, ctx);
        assertEquals(YamlNode.scalar(true), result);
    }

    @Test
    void toYamlString() {
        YamlNode result = adapter.toYaml("hello", String.class, ctx);
        assertEquals(YamlNode.scalar("hello"), result);
    }

    @Test
    void toYamlDouble() {
        YamlNode result = adapter.toYaml(3.14, double.class, ctx);
        assertEquals(YamlNode.scalar(3.14), result);
    }

    @Test
    void toYamlNull() {
        YamlNode result = adapter.toYaml(null, int.class, ctx);
        assertEquals(YamlNode.scalar(null), result);
    }
}
