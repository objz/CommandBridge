package dev.objz.commandbridge.main.scripting.v3;

import dev.objz.commandbridge.main.scripting.v3.compile.api.Registry;
import dev.objz.commandbridge.main.scripting.v3.compile.core.CollectingProblemSink;
import dev.objz.commandbridge.main.scripting.v3.compile.core.CompileContext;
import dev.objz.commandbridge.main.scripting.v3.compile.core.Path;
import dev.objz.commandbridge.main.scripting.v3.compile.nodes.*;
import dev.objz.commandbridge.main.scripting.v3.model.raw.*;
import dev.objz.commandbridge.main.scripting.v3.model.resolved.Script;
import dev.objz.commandbridge.main.scripting.v3.serialize.DurationSerializer;

import java.time.Duration;

import org.spongepowered.configurate.BasicConfigurationNode;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.objectmapping.ObjectMapper;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializerCollection;

public final class ScriptLoader {

	private final ObjectMapper.Factory mapperFactory;

	public ScriptLoader(ObjectMapper.Factory mapperFactory) {
		this.mapperFactory = mapperFactory;
	}

	public static final class Result {
		public final Script script; // null if errors
		public final CollectingProblemSink sink; // always present

		Result(Script script, CollectingProblemSink sink) {
			this.script = script;
			this.sink = sink;
		}
	}

	public Result load(ConfigurationNode node, String sourceName) throws SerializationException {
		var serializers = TypeSerializerCollection.defaults()
				.childBuilder()
				.register(Duration.class, DurationSerializer.INSTANCE)
				.build();

		ConfigurationOptions opts = node.options().serializers(serializers);
		ConfigurationNode patched = BasicConfigurationNode.root(opts);
		patched.mergeFrom(node);

		var mapper = mapperFactory.get(ScriptRaw.class);
		var raw = mapper.load(patched);

		// Build the node-compiler registry
		Registry reg = new Registry();
		// Root & groups
		reg.register(ScriptRaw.class,
				new dev.objz.commandbridge.main.scripting.v3.compile.nodes.ScriptCompiler());
		reg.register(PermissionsRaw.class, new PermissionsCompiler());
		reg.register(DefaultsRaw.class, new DefaultsCompiler());
		// Target graph:
		// - TargetRaw: register override-mode for general use; DefaultsCompiler will
		// invoke default-mode directly
		reg.register(TargetRaw.class, new TargetCompiler(false));
		// - TargetKind override-mode for general use; TargetCompiler(default) invokes
		// its own default-mode
		reg.register(TargetKindRaw.class, new TargetKindCompiler(false));
		reg.register(TargetServerRaw.class, new TargetServerCompiler());
		// Lists
		reg.register(ArgRaw.ListWrapper.class, new ArgListCompiler());
		reg.register(CommandStepRaw.ListWrapper.class, new CommandListCompiler());

		var sink = new CollectingProblemSink();
		var ctx = new CompileContext(reg, null);

		Script script = ctx.compile(ScriptRaw.class, raw, sink, Path.root());
		return new Result(script, sink);
	}
}
