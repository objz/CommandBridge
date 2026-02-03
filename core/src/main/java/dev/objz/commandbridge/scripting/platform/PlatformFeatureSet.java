package dev.objz.commandbridge.scripting.platform;

import dev.objz.commandbridge.scripting.model.enums.Location;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class PlatformFeatureSet implements PlatformFeatures {
	private final Map<Location, Set<String>> features;

	public PlatformFeatureSet(Map<Location, Set<String>> features) {
		this.features = new EnumMap<>(Location.class);
		if (features != null) {
			for (Map.Entry<Location, Set<String>> entry : features.entrySet()) {
				if (entry.getKey() == null || entry.getValue() == null) {
					continue;
				}
				this.features.put(entry.getKey(), Set.copyOf(entry.getValue()));
			}
		}
	}

	@Override
	public boolean isEnabled(Location location, String feature) {
		if (location == null || feature == null || feature.isBlank()) {
			return false;
		}
		Set<String> locationFeatures = features.get(location);
		if (locationFeatures == null || locationFeatures.isEmpty()) {
			return false;
		}
		return locationFeatures.contains(feature);
	}

	public static Builder builder() {
		return new Builder();
	}

	public static PlatformFeatureSet empty() {
		return new PlatformFeatureSet(Map.of());
	}

	public static final class Builder {
		private final Map<Location, Set<String>> features = new EnumMap<>(Location.class);

		public Builder add(Location location, String feature) {
			if (location == null) {
				throw new IllegalArgumentException("Location cannot be null");
			}
			if (feature == null || feature.isBlank()) {
				throw new IllegalArgumentException("Feature cannot be null or blank");
			}
			features.computeIfAbsent(location, key -> new HashSet<>()).add(feature);
			return this;
		}

		public PlatformFeatureSet build() {
			return new PlatformFeatureSet(features);
		}
	}
}
