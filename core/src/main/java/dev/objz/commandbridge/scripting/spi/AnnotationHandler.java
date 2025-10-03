package dev.objz.commandbridge.scripting.spi;

import java.lang.annotation.Annotation;

public interface AnnotationHandler<A extends Annotation> {
	Class<A> annotationType();
}
