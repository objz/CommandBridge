package dev.objz.commandbridge.main.config.profile;

import java.util.Set;

public interface ConfigProfile<T> {
	T defaults();

	Set<String> validKeys();

	Result<T> normalize(T in);

	record Result<T>(T config, boolean ok) {} 
}
