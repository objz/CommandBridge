package dev.objz.commandbridge.main.config.profile;

import java.util.Set;

public interface ConfigProfile<T> {
	T defaults();

	Set<String> validKeys();

	boolean validate(T cfg);
}
