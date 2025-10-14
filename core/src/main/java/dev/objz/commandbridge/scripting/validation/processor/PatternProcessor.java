package dev.objz.commandbridge.scripting.validation.processor;

import dev.objz.commandbridge.scripting.anno.Pattern;
import dev.objz.commandbridge.scripting.bind.BindContext;
import dev.objz.commandbridge.scripting.bind.RecordBinder;
import dev.objz.commandbridge.scripting.validation.PostProcessor;

import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.RecordComponent;
import java.util.Collection;
import java.util.regex.Matcher;

public final class PatternProcessor implements PostProcessor {

	@Override
	public void process(RecordBinder.MutableRecordBuffer buf, BindContext ctx) {
		RecordComponent[] comps = buf.components();
		for (int i = 0; i < comps.length; i++) {
			RecordComponent comp = comps[i];

			Pattern fieldPat = comp.getAnnotation(Pattern.class);

			Pattern elementPat = extractElementPattern(comp.getAnnotatedType());

			if (fieldPat == null && elementPat == null) {
				continue;
			}

			Object v = buf.get(i);
			if (v == null) {
				continue;
			}

			String field = comp.getName();

			if (fieldPat != null && v instanceof CharSequence cs) {
				validateOne(field, cs.toString(), fieldPat, ctx);
				continue;
			}

			if (elementPat != null) {
				if (v instanceof Collection<?> col) {
					int idx = 0;
					for (Object e : col) {
						if (e instanceof CharSequence ecs) {
							validateOne(field + "[" + idx + "]", ecs.toString(), elementPat,
									ctx);
						} else if (e != null) {
							ctx.problems().error(field,
									"@Pattern on element type requires elements to be String, got "
											+ e.getClass().getSimpleName());
						}
						idx++;
					}
					continue;
				}
				if (v.getClass().isArray()) {
					int len = java.lang.reflect.Array.getLength(v);
					for (int idx = 0; idx < len; idx++) {
						Object e = java.lang.reflect.Array.get(v, idx);
						if (e instanceof CharSequence ecs) {
							validateOne(field + "[" + idx + "]", ecs.toString(), elementPat,
									ctx);
						} else if (e != null) {
							ctx.problems().error(field,
									"@Pattern on element type requires elements to be String, got "
											+ e.getClass().getSimpleName());
						}
					}
					continue;
				}
				ctx.problems().error(field,
						"@Pattern on element type is only applicable to collections/arrays");
				continue;
			}

			if (fieldPat != null && !(v instanceof CharSequence)) {
				ctx.problems().error(field,
						"@Pattern allowed only on String fields (got "
								+ v.getClass().getSimpleName() + ")");
			}
		}
	}

	private static Pattern extractElementPattern(AnnotatedType annotatedType) {
		if (annotatedType instanceof AnnotatedParameterizedType apt) {
			AnnotatedType[] args = apt.getAnnotatedActualTypeArguments();
			if (args.length == 1) {
				return args[0].getAnnotation(Pattern.class);
			}
		}
		return null;
	}

	private static void validateOne(String where, String s, Pattern ann, BindContext ctx) {
		try {
			java.util.regex.Pattern p = java.util.regex.Pattern.compile(ann.regex(), ann.flags());
			Matcher m = p.matcher(s);
			if (!m.matches()) {
				String msg = ann.message().isBlank()
						? "does not match required pattern: " + ann.regex()
						: ann.message();
				ctx.problems().error(where, msg);
			}
		} catch (Exception e) {
			ctx.problems().error(where, "Invalid @Pattern regex: " + e.getMessage());
		}
	}
}
