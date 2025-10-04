package dev.objz.commandbridge.scripting.anno;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Takes two valuse from different classes and merges them. It will take the
 * 'default' value
 * as argument and override them if a value is present of the variable this anno
 * is applied to.
 * it will override the default value for that field if there is value present
 * if not just uses
 * the default value.
 */
@Retention(RUNTIME)
@Target(TYPE)
public @interface Merge {
}
