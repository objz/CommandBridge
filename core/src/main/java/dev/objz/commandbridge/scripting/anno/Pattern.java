package dev.objz.commandbridge.scripting.anno;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.RECORD_COMPONENT;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)
@Target({ RECORD_COMPONENT, FIELD, TYPE_USE})
public @interface Pattern {
	String regex();

	int flags() default 0; 

	String message() default "does not match required pattern";
}
