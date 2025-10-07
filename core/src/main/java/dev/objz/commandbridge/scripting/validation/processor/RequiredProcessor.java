package dev.objz.commandbridge.scripting.validation.processor;

import dev.objz.commandbridge.scripting.bind.BindContext;
import dev.objz.commandbridge.scripting.bind.RecordBinder;
import dev.objz.commandbridge.scripting.validation.PostProcessor;

import java.lang.reflect.RecordComponent;

public final class RequiredProcessor implements PostProcessor {
	@Override
	public void process(RecordBinder.MutableRecordBuffer buf, BindContext ctx) {
		RecordComponent[] comps = buf.components();
		for (int i = 0; i < comps.length; i++) {
			var reqOpt = buf.requiredOf(i);
			if (reqOpt.isEmpty())
				continue;
			if (buf.get(i) == null) {
				String field = comps[i].getName();
				ctx.problems().error(field, reqOpt.get().message());
			}
		}
	}
}
