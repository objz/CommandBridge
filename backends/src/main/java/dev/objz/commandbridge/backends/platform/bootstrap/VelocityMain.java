package dev.objz.commandbridge.backends.platform.bootstrap;

import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

import java.nio.file.Path;

public final class VelocityMain {
	private final ProxyServer proxy;
	private final Logger logger;
	private final Path dataDirectory;
	private final Object pluginInstance;

	public VelocityMain(ProxyServer proxy, Logger logger, Path dataDirectory, Object pluginInstance) {
		this.proxy = proxy;
		this.logger = logger;
		this.dataDirectory = dataDirectory;
		this.pluginInstance = pluginInstance;
	}

	public ProxyServer getProxy() {
		return proxy;
	}

	public Logger getLogger() {
		return logger;
	}

	public Path getDataDirectory() {
		return dataDirectory;
	}

	public Object getPluginInstance() {
		return pluginInstance;
	}
}
