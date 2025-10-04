package dev.objz.commandbridge.scripting.bind.adapters;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import dev.objz.commandbridge.scripting.bind.ConvertContext;
import dev.objz.commandbridge.scripting.bind.RecordBinder;
import dev.objz.commandbridge.scripting.bind.TypeAdapter;
import dev.objz.commandbridge.scripting.yaml.YamlNode;

public final class ListAdapter implements TypeAdapter<List<?>> {

	@Override
	public boolean supports(Type targetType) {
		if (targetType instanceof ParameterizedType pt) {
			return pt.getRawType() == List.class;
		}
		return false;
	}

	@Override
	public List<?> fromYaml(YamlNode node, Type targetType, ConvertContext ctx) {
		if (!(node instanceof YamlNode.Sequence seq)) {
			throw new IllegalArgumentException("Expected sequence for List");
		}
		ParameterizedType pt = (ParameterizedType) targetType;
		Type elemType = pt.getActualTypeArguments()[0];

		var elemAdapter = ctx.adapters().find(elemType);
		List<Object> out = new ArrayList<>(seq.elements().size());

		String base = RecordBinder.currentPath();
		int idx = 0;
		for (YamlNode child : seq.elements()) {
			String prev = RecordBinder.currentPath();
			try {
				String elemPath = (base == null || base.isBlank()) ? ("[" + idx + "]")
						: (base + "[" + idx + "]");
				RecordBinder.setCurrentPath(elemPath);
				out.add(elemAdapter.fromYaml(child, elemType, ctx));
			} catch (Exception ex) {
				ctx.problems().error((base != null ? base : "list") + "[" + idx + "]",
						"Element conversion failed: " + ex.getMessage());
				out.add(null);
			} finally {
				RecordBinder.setCurrentPath(prev);
			}
			idx++;
		}
		return List.copyOf(out);
	}

	@Override
	public YamlNode toYaml(List<?> value, Type targetType, ConvertContext ctx) {
		ParameterizedType pt = (ParameterizedType) targetType;
		Type elemType = pt.getActualTypeArguments()[0];
		var elemAdapter = ctx.adapters().find(elemType);
		List<YamlNode> out = new ArrayList<>();
		if (value != null) {
			for (Object v : value)
				out.add(elemAdapter.toYaml(v, elemType, ctx));
		}
		return YamlNode.sequence(out);
	}
}
