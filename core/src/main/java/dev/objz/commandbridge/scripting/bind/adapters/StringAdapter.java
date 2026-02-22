package dev.objz.commandbridge.scripting.bind.adapters;

import dev.objz.commandbridge.scripting.bind.ConvertContext;
import dev.objz.commandbridge.scripting.bind.TypeAdapter;
import dev.objz.commandbridge.scripting.yaml.YamlNode;

import java.lang.reflect.Type;

public final class StringAdapter implements TypeAdapter<String> {
    @Override
    public boolean supports(Type targetType) {
        return targetType == String.class;
    }

    @Override
    public String fromYaml(YamlNode node, Type targetType, ConvertContext ctx) {
        if (node instanceof YamlNode.Scalar s) {
            Object v = s.value();
            return v == null ? null : String.valueOf(v);
        }
        throw new IllegalArgumentException("Expected scalar string");
    }

    @Override
    public YamlNode toYaml(String value, Type targetType, ConvertContext ctx) {
        return YamlNode.scalar(value);
    }
}
