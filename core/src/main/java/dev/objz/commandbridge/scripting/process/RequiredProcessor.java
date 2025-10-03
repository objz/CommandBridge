package dev.objz.commandbridge.scripting.process;

import dev.objz.commandbridge.scripting.bind.BindContext;
import dev.objz.commandbridge.scripting.bind.RecordBinder;

public final class RequiredProcessor implements PostProcessor {
	@Override
	public void process(RecordBinder.MutableRecordBuffer buf, BindContext ctx) {
		var comps = buf.components();
		for (int i = 0; i < comps.length; i++) {
			var reqOpt = buf.requiredOf(i);
			if (reqOpt.isEmpty())
				continue;
			if (buf.get(i) == null) {
				ctx.problems().error(buf.pathOf(i), "is required");
			}
		}
	}
}
