package dev.objz.commandbridge.scripting.platform;

import dev.objz.commandbridge.scripting.model.enums.Location;

public interface PlatformFeatures {
	boolean isEnabled(Location location, String feature);

	static PlatformFeatures none() {
		return (location, feature) -> false;
	}
}
