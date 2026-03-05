package dev.objz.commandbridge.scripting.anno;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Marks ArgType values that can resolve to a player identity (name or UUID)
 * Used by the player-arg setting in the server block
 */
@Retention(RUNTIME)
@Target(FIELD)
public @interface PlayerResolvable {
}
