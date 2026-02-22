package dev.objz.commandbridge.scripting.anno;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.RECORD_COMPONENT;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * If the annotated field is null, take the value from a "defaults" provider
 * field on the parent aggregate
 *
 * record CmdMapping(@Merge RunAs runAs, @Merge Duration delay, ...) {}
 *
 * Behavior:
 * runAs := (cmd.runAs != null ? cmd.runAs : defaults.runAs)
 */
@Retention(RUNTIME)
@Target({ RECORD_COMPONENT, FIELD })
public @interface Merge {
    String from() default "defaults";
}
