package dev.objz.commandbridge.scripting.spi;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

public final class AnnotationHandlerRegistry {
	private final List<AnnotationHandler<?>> handlers = new ArrayList<>();

	public AnnotationHandlerRegistry register(AnnotationHandler<?> h) {
		handlers.add(h);
		return this;
	}

	public <A extends Annotation> List<AnnotationHandler<A>> find(Class<A> type) {
		List<AnnotationHandler<A>> out = new ArrayList<>();
		for (AnnotationHandler<?> h : handlers) {
			if (h.annotationType() == type)
				out.add((AnnotationHandler<A>) h);
		}
		return out;
	}
}
