package dev.objz.commandbridge.main.scripting;

import dev.objz.commandbridge.main.scripting.model.Spec;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public final class ScriptLoader {
	public List<Spec.ScriptSpecV2> loadAll(Path dir) throws IOException {
		if (!Files.isDirectory(dir))
			return List.of();
		List<Spec.ScriptSpecV2> out = new ArrayList<>();
		try (Stream<Path> s = Files.walk(dir, 1)) {
			for (Path p : s.filter(Files::isRegularFile).toList()) {
				String fn = p.getFileName().toString().toLowerCase();
				if (!(fn.endsWith(".yml") || fn.endsWith(".yaml")))
					continue;
				out.add(load(p));
			}
		}
		return out;
	}

	public Spec.ScriptSpecV2 load(Path file) throws IOException {
		var loader = YamlConfigurationLoader.builder()
				.path(file)
				.nodeStyle(NodeStyle.BLOCK)
				.build();
		ConfigurationNode root = loader.load();
		return root.get(Spec.ScriptSpecV2.class);
	}
}
