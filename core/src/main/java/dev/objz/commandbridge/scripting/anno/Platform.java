package dev.objz.commandbridge.scripting.anno;

import dev.objz.commandbridge.scripting.model.enums.Location;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Platform {
    Location[] value();

	OptionalSupport[] optional() default {};

	@interface OptionalSupport {
		Location location();
		String feature();
	}
}
