package dev.objz.commandbridge.scripting.validation.processor;

import dev.objz.commandbridge.scripting.anno.Min;
import dev.objz.commandbridge.scripting.bind.BindContext;
import dev.objz.commandbridge.scripting.bind.RecordBinder;
import dev.objz.commandbridge.scripting.validation.PostProcessor;

import java.lang.reflect.RecordComponent;
import java.time.Duration;

public final class MinProcessor implements PostProcessor {

	@Override
	public void process(RecordBinder.MutableRecordBuffer buf, BindContext ctx) {
		RecordComponent[] comps = buf.components();
		for (int i = 0; i < comps.length; i++) {
			Min ann = comps[i].getAnnotation(Min.class);
			if (ann == null)
				continue;

			Object v = buf.get(i);
			if (v == null)
				continue;

			String field = comps[i].getName();
			long min = ann.value();

			if (v instanceof Number n) {
				long lv = n.longValue();
				if (lv < min) {
					String msg = ann.message().isBlank() ? ("must be >= " + min) : ann.message();
					ctx.problems().error(field, msg);
				}
				continue;
			}
			if (v instanceof Duration d) {
				if (d.compareTo(Duration.ofSeconds(min)) < 0) {
					String msg = ann.message().isBlank() ? ("must be >= " + min + "s")
							: ann.message();
					ctx.problems().error(field, msg);
				}
				continue;
			}

			ctx.problems().error(field,
					"@Min allowed only on numeric or Duration fields (got "
							+ v.getClass().getSimpleName() + ")");
		}
	}
}
