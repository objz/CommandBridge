package dev.objz.commandbridge.config.profile;

public interface ConfigProfile<T> {
	T defaults();

	Result<T> normalize(T in);

	record Result<T>(T config, boolean ok) {
	}
}
