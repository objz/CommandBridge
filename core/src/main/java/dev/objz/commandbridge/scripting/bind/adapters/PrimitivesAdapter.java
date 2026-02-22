package dev.objz.commandbridge.scripting.bind.adapters;

import dev.objz.commandbridge.scripting.bind.ConvertContext;
import dev.objz.commandbridge.scripting.bind.TypeAdapter;
import dev.objz.commandbridge.scripting.yaml.YamlNode;

import java.lang.reflect.Type;

public final class PrimitivesAdapter implements TypeAdapter<Object> {

    @Override
    public boolean supports(Type targetType) {
        if (!(targetType instanceof Class<?> c))
            return false;
        return c.isPrimitive()
                || c == Integer.class
                || c == Long.class
                || c == Short.class
                || c == Byte.class
                || c == Double.class
                || c == Float.class
                || c == Boolean.class
                || c == Character.class;
    }

    @Override
    public Object fromYaml(YamlNode node, Type targetType, ConvertContext ctx) {
        if (!(targetType instanceof Class<?> c))
            throw new IllegalArgumentException("Target not a class");
        Object v = (node instanceof YamlNode.Scalar s) ? s.value() : null;
        if (v == null)
            return null;

        try {
            if (c == int.class || c == Integer.class)
                return toInt(v);
            if (c == long.class || c == Long.class)
                return toLong(v);
            if (c == short.class || c == Short.class)
                return toShort(v);
            if (c == byte.class || c == Byte.class)
                return toByte(v);
            if (c == double.class || c == Double.class)
                return toDouble(v);
            if (c == float.class || c == Float.class)
                return toFloat(v);
            if (c == boolean.class || c == Boolean.class)
                return toBool(v);
            if (c == char.class || c == Character.class)
                return toChar(v);
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException(
                    "Cannot convert " + v + " to " + c.getSimpleName() + ": " + ex.getMessage());
        }
        throw new IllegalArgumentException("Unsupported primitive wrapper: " + c.getName());
    }

    @Override
    public YamlNode toYaml(Object value, Type targetType, ConvertContext ctx) {
        return YamlNode.scalar(value);
    }

    private static int toInt(Object v) {
        return v instanceof Number n ? n.intValue() : Integer.parseInt(v.toString());
    }

    private static long toLong(Object v) {
        return v instanceof Number n ? n.longValue() : Long.parseLong(v.toString());
    }

    private static short toShort(Object v) {
        return v instanceof Number n ? n.shortValue() : Short.parseShort(v.toString());
    }

    private static byte toByte(Object v) {
        return v instanceof Number n ? n.byteValue() : Byte.parseByte(v.toString());
    }

    private static double toDouble(Object v) {
        return v instanceof Number n ? n.doubleValue() : Double.parseDouble(v.toString());
    }

    private static float toFloat(Object v) {
        return v instanceof Number n ? n.floatValue() : Float.parseFloat(v.toString());
    }

    private static boolean toBool(Object v) {
        if (v instanceof Boolean b)
            return b;
        String s = v.toString().trim().toLowerCase();
        return switch (s) {
            case "true", "yes", "1", "on" -> true;
            case "false", "no", "0", "off" -> false;
            default -> throw new IllegalArgumentException("not a boolean: " + v);
        };
    }

    private static char toChar(Object v) {
        String s = v.toString();
        if (s.length() != 1)
            throw new IllegalArgumentException("not a single character: " + v);
        return s.charAt(0);
    }
}
