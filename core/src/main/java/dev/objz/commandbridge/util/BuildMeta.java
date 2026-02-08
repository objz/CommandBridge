package dev.objz.commandbridge.util;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class BuildMeta {

	public static final String VERSION;

	static {
		String v = "unknown";
		try (InputStream in = BuildMeta.class.getResourceAsStream("/version")) {
			if (in != null) {
				v = new String(in.readAllBytes(), StandardCharsets.UTF_8).trim();
			}
		} catch (Exception ignored) {
		}
		VERSION = v;
	}

	private BuildMeta() {
	}
}
