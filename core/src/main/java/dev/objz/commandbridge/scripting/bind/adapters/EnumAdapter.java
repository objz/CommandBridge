package dev.objz.commandbridge.scripting.bind.adapters;

import java.lang.reflect.Type;

import dev.objz.commandbridge.scripting.bind.ConvertContext;
import dev.objz.commandbridge.scripting.bind.TypeAdapter;
import dev.objz.commandbridge.scripting.yaml.YamlNode;

public final class EnumAdapter implements TypeAdapter<Enum<?>> {

    @Override
    public boolean supports(Type targetType) {
        return targetType instanceof Class<?> c && c.isEnum();
    }

    @Override
    public Enum<?> fromYaml(YamlNode node, Type targetType, ConvertContext ctx) {
        if (!(targetType instanceof Class<?> c) || !c.isEnum())
            throw new IllegalArgumentException("Target not enum");
        if (!(node instanceof YamlNode.Scalar s))
            throw new IllegalArgumentException("Expected scalar for enum");
        Object v = s.value();
        if (v == null)
            return null;
        String name = v.toString();
        for (Object constant : c.getEnumConstants()) {
            if (constant instanceof Enum<?> e && e.name().equalsIgnoreCase(name)) {
                return e;
            }
        }
        Enum<?> exact = exactValueOf(c, name);
        if (exact != null) {
            return exact;
        }
        throw new IllegalArgumentException(
                "Unknown enum constant: " + name + " for " + c.getSimpleName());
    }

    @SuppressWarnings("unchecked")
    private static <E extends Enum<E>> E exactValueOf(Class<?> enumClass, String name) {
        try {
            return Enum.valueOf((Class<E>) enumClass, name);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    @Override
    public YamlNode toYaml(Enum<?> value, Type targetType, ConvertContext ctx) {
        return YamlNode.scalar(value == null ? null : value.name());
    }
}
