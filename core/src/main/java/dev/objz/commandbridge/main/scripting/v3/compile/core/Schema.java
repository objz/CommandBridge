package dev.objz.commandbridge.main.scripting.v3.compile.core;

import java.time.Duration;
import java.util.List;

public final class Schema {
	private Schema() {
	}

	// Script
	public static final Rule<Integer> VERSION = Rule.isRequired();
	public static final Rule<String> NAME = Rule.isRequired();
	public static final Rule<String> DESCRIPTION = Rule.isOptional(null);
	public static final Rule<Boolean> ENABLED = Rule.isOptional(Boolean.TRUE);
	public static final Rule<List<String>> ALIASES = Rule.isOptional(List.of());

	// Permissions
	public static final Rule<Boolean> PERM_ENABLED = Rule.isOptional(Boolean.FALSE);
	public static final Rule<Boolean> PERM_SILENT = Rule.isOptional(Boolean.FALSE);

	// Defaults → Target
	public static final Rule<Boolean> TARGET_SERVER_REQUIRED = Rule.isOptional(Boolean.FALSE);
	public static final Rule<Boolean> TARGET_SERVER_SCHEDULE = Rule.isOptional(Boolean.FALSE);
	public static final Rule<Duration> TARGET_SERVER_TIMEOUT = Rule.isOptional(Duration.ofSeconds(1));
	public static final Rule<Duration> TARGET_SERVER_FREQ = Rule.isOptional(Duration.ofSeconds(2));

	// Defaults → global timings
	public static final Rule<Duration> DEFAULT_DELAY = Rule.isOptional(Duration.ZERO);
	public static final Rule<Duration> DEFAULT_COOLDOWN = Rule.isOptional(Duration.ZERO);

	// CommandStep
	public static final Rule<String> STEP_COMMAND = Rule.isRequired();
	public static final Rule<Duration> STEP_DELAY = Rule.isOptional(null); // null means inherit
	public static final Rule<Duration> STEP_TIMEOUT = Rule.isOptional(null); // null means inherit
}
