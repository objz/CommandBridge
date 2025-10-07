package dev.objz.commandbridge.scripting.validation.processor;

import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;

import dev.objz.commandbridge.scripting.anno.Default;
import dev.objz.commandbridge.scripting.bind.BindContext;
import dev.objz.commandbridge.scripting.bind.RecordBinder;
import dev.objz.commandbridge.scripting.bind.TypeAdapter;
import dev.objz.commandbridge.scripting.validation.PostProcessor;
import dev.objz.commandbridge.scripting.yaml.YamlNode;

public final class DefaultProcessor implements PostProcessor {

	@Override
	public void process(RecordBinder.MutableRecordBuffer buf, BindContext ctx) {
		RecordComponent[] comps = buf.components();
		for (int i = 0; i < comps.length; i++) {
			Object cur = buf.get(i);
			if (cur != null)
				continue;
			var defOpt = buf.defaultOf(i);
			if (defOpt.isEmpty())
				continue;

			Default def = defOpt.get();
			Type t = buf.typeOf(i);
			TypeAdapter<?> adapter = ctx.adapters().find(t);
			try {
				var node = YamlNode.scalar(def.value());
				Object converted = adapter.fromYaml(node, t, ctx);
				buf.set(i, converted);
			} catch (Exception ex) {
				String field = comps[i].getName();
				ctx.problems().error(field,
						"Invalid default '" + def.value() + "': " + ex.getMessage());
			}
		}
	}
}
