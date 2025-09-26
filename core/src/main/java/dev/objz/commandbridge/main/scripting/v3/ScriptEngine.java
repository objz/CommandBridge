package dev.objz.commandbridge.main.scripting.v3;

import dev.objz.commandbridge.main.logging.Log;
import dev.objz.commandbridge.main.scripting.v3.effective.EffectiveModels;
import dev.objz.commandbridge.main.scripting.v3.loader.KeyNormalizer;
import dev.objz.commandbridge.main.scripting.v3.loader.PathIntrospector;
import dev.objz.commandbridge.main.scripting.v3.model.ScriptSpec;
import dev.objz.commandbridge.main.scripting.v3.model.args.ArgDef;
import dev.objz.commandbridge.main.scripting.v3.model.args.ArgDefSerializer;
import dev.objz.commandbridge.main.scripting.v3.resolver.*;
import io.leangen.geantyref.TypeToken;

import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

public final class ScriptEngine {
	private final SpecValidator validator = new SpecValidator();
	private final DefaultsResolver defaults = new DefaultsResolver();
	private final ArgsResolver args = new ArgsResolver();

	public List<EffectiveModels.Script> loadAll(Path dir) throws IOException {
		if (!Files.isDirectory(dir))
			return List.of();
		List<EffectiveModels.Script> out = new ArrayList<>();
		try (Stream<Path> s = Files.walk(dir, 1)) {
			for (Path p : s.filter(Files::isRegularFile).toList()) {
				String fn = p.getFileName().toString().toLowerCase(Locale.ROOT);
				if (!(fn.endsWith(".yml") || fn.endsWith(".yaml")))
					continue;
				var loaded = load(p);
				if (loaded != null)
					out.add(loaded);
			}
		}
		return java.util.List.copyOf(out);
	}

	public EffectiveModels.Script load(Path file) throws IOException {
		var loader = YamlConfigurationLoader.builder()
				.path(file)
				.nodeStyle(NodeStyle.BLOCK)
				.defaultOptions(opts -> opts.serializers(s -> s
						.registerExact(TypeToken.get(ArgDef.class), new ArgDefSerializer())))
				.build();
		ConfigurationNode root = loader.load();

		Set<String> allowed = new PathIntrospector().allowedPaths(ScriptSpec.class);

		new KeyNormalizer(allowed).normalize(root);

		ScriptSpec raw = root.get(ScriptSpec.class);
		if (raw == null) {
			Log.error("Script '{}': could not parse", file.getFileName());
			return null;
		}

		try {
			validator.validate(raw);
		} catch (SpecValidator.Errors ex) {
			for (String e : ex.list())
				Log.error("Script '{}': {}", ex.script(), e);
			return null; // hard fail
		}

		var perms = defaults.perms(raw.permissions());
		var defs = defaults.defaults(raw.defaults());
		var effArgs = args.toEffective(raw.args());

		var steps = new ArrayList<EffectiveModels.Step>();
		for (int i = 0; i < raw.commands().size(); i++) {
			var st = raw.commands().get(i);
			var t = defaults.mergeStepTarget(defs.target(), st.target());
			var delay = dev.objz.commandbridge.main.scripting.v3.util.DurationUtil.parseFlexible(st.delay(),
					defs.delay());
			var timeout = dev.objz.commandbridge.main.scripting.v3.util.DurationUtil
					.parseFlexible(st.timeout(), defs.timeout());
			steps.add(new EffectiveModels.Step(st.command(), t, delay, timeout));
		}

		int version = raw.version() == null ? 2 : raw.version();
		String name = raw.name();
		String description = raw.description() == null ? "" : raw.description();
		boolean enabled = raw.enabled() == null ? true : raw.enabled();
		java.util.List<String> aliases = raw.aliases() == null ? java.util.List.of()
				: java.util.List.copyOf(raw.aliases());

		return new EffectiveModels.Script(version, name, description, enabled, aliases, perms, defs, effArgs,
				java.util.List.copyOf(steps));
	}
}
