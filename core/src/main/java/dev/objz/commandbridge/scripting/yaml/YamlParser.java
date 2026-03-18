package dev.objz.commandbridge.scripting.yaml;

import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class YamlParser {

    private final Load loader;

    public YamlParser() {
        this.loader = new Load(LoadSettings.builder().build());
    }

    public YamlNode parse(InputStream in) {
        Object root = loader.loadFromInputStream(in);
        return toYamlNode(root);
    }

    public YamlNode parse(String text) {
        Object root = loader.loadFromString(text);
        return toYamlNode(root);
    }

    private YamlNode toYamlNode(Object o) {
        if (o == null)
            return YamlNode.scalar(null);
        if (o instanceof String || o instanceof Number || o instanceof Boolean) {
            return YamlNode.scalar(o);
        }
        if (o instanceof Map<?, ?> map) {
            Map<String, YamlNode> out = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : map.entrySet()) {
                out.put(String.valueOf(e.getKey()), toYamlNode(e.getValue()));
            }
            return YamlNode.mapping(out);
        }
        if (o instanceof Iterable<?> it) {
            List<YamlNode> list = new ArrayList<>();
            for (Object e : it)
                list.add(toYamlNode(e));
            return YamlNode.sequence(list);
        }
        return YamlNode.scalar(String.valueOf(o));
    }
}
