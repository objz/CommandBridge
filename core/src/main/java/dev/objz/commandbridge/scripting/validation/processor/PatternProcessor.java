package dev.objz.commandbridge.scripting.validation.processor;

import dev.objz.commandbridge.scripting.anno.Pattern;
import dev.objz.commandbridge.scripting.bind.BindContext;
import dev.objz.commandbridge.scripting.bind.RecordBinder;
import dev.objz.commandbridge.scripting.validation.PostProcessor;

import java.lang.reflect.RecordComponent;

public final class PatternProcessor implements PostProcessor {

	@Override
	public void process(RecordBinder.MutableRecordBuffer buf, BindContext ctx) {
		RecordComponent[] comps = buf.components();
		for (int i = 0; i < comps.length; i++) {
			var ann = comps[i].getAnnotation(Pattern.class);
			if (ann == null)
				continue;

			Object v = buf.get(i);
			if (v == null)
				continue;

			String field = comps[i].getName();

			if (!(v instanceof CharSequence cs)) {
				ctx.problems().error(field,
						"@Pattern allowed only on String fields (got "
								+ v.getClass().getSimpleName() + ")");
				continue;
			}
			String s = cs.toString();
			try {
				java.util.regex.Pattern p = java.util.regex.Pattern.compile(ann.regex(), ann.flags());
				if (!p.matcher(s).matches()) {
					String msg = ann.message().isBlank()
							? "does not match required pattern: " + ann.regex()
							: ann.message();
					ctx.problems().error(field, msg);
				}
			} catch (Exception e) {
				ctx.problems().error(field, "Invalid @Pattern regex: " + e.getMessage());
			}
		}
	}
}
