package dev.objz.commandbridge.main.scripting.v3.compile.api;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("rawtypes")
public final class Registry {
	private final Map<Class, NodeCompiler> map = new ConcurrentHashMap<>();

	public <R, T> void register(Class<R> raw, NodeCompiler<R, T> compiler) {
		map.put(raw, compiler);
	}

	public <R, T> NodeCompiler<R, T> get(Class<R> raw) {
		var c = (NodeCompiler<R, T>) map.get(raw);
		if (c == null) {
			throw new IllegalStateException("No compiler registered for " + raw.getName());
		}
		return c;
	}
}
