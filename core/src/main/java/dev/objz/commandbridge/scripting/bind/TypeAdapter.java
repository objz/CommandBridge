package dev.objz.commandbridge.scripting.bind;

import java.lang.reflect.Type;

import dev.objz.commandbridge.scripting.yaml.YamlNode;

public interface TypeAdapter<T> {
	boolean supports(Type targetType);

	T fromYaml(YamlNode node, Type targetType, ConvertContext ctx);

	YamlNode toYaml(T value, Type targetType, ConvertContext ctx);
}
