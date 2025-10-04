package dev.objz.commandbridge.scripting.anno;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.RECORD_COMPONENT;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)
@Target({ RECORD_COMPONENT, FIELD })
public @interface Max {
	long value();

	String message() default "is above maximum";
}
