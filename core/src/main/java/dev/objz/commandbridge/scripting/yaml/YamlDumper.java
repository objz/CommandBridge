package dev.objz.commandbridge.scripting.yaml;

import org.snakeyaml.engine.v2.api.Dump;
import org.snakeyaml.engine.v2.api.DumpSettings;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class YamlDumper {

    private final Dump dumper;

    public YamlDumper() {
        var settings = DumpSettings.builder()
                .setDefaultScalarStyle(org.snakeyaml.engine.v2.common.ScalarStyle.PLAIN)
                .setDefaultFlowStyle(org.snakeyaml.engine.v2.common.FlowStyle.BLOCK)
                .build();
        this.dumper = new Dump(settings);
    }

    public String dump(YamlNode root) {
        Object javaValue = fromYamlNode(root);
        return dumper.dumpToString(javaValue);
    }

    private Object fromYamlNode(YamlNode node) {
        if (node instanceof YamlNode.Scalar s) {
            return s.value();
        }
        if (node instanceof YamlNode.Mapping m) {
            Map<String, Object> out = new LinkedHashMap<>();
            m.entries().forEach((k, v) -> out.put(k, fromYamlNode(v)));
            return out;
        }
        if (node instanceof YamlNode.Sequence seq) {
            List<Object> list = new ArrayList<>(seq.elements().size());
            for (YamlNode n : seq.elements())
                list.add(fromYamlNode(n));
            return list;
        }
        return null;
    }
}
