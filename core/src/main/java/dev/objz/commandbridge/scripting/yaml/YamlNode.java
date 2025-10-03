package dev.objz.commandbridge.scripting.yaml;

import java.util.List;
import java.util.Map;

public sealed interface YamlNode permits YamlNode.Scalar, YamlNode.Mapping, YamlNode.Sequence {

	record Scalar(Object value) implements YamlNode {
	}

	record Mapping(Map<String, YamlNode> entries) implements YamlNode {
	}

	record Sequence(List<YamlNode> elements) implements YamlNode {
	}

	static Scalar scalar(Object value) {
		return new Scalar(value);
	}

	static Mapping mapping(Map<String, YamlNode> entries) {
		return new Mapping(Map.copyOf(entries));
	}

	static Sequence sequence(List<YamlNode> elements) {
		return new Sequence(List.copyOf(elements));
	}
}
