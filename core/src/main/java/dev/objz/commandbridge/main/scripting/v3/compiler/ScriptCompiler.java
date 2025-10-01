package dev.objz.commandbridge.main.scripting.v3.compiler;

import dev.objz.commandbridge.main.scripting.v3.compiler.compilers.*;
import dev.objz.commandbridge.main.scripting.v3.compiler.io.DurationSerializer;
import dev.objz.commandbridge.main.scripting.v3.compiler.problems.CollectingProblemSink;
import dev.objz.commandbridge.main.scripting.v3.compiler.schema.Path;
import dev.objz.commandbridge.main.scripting.v3.model.dto.*;
import dev.objz.commandbridge.main.scripting.v3.model.domain.Script;

import java.time.Duration;

import org.spongepowered.configurate.BasicConfigurationNode;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.objectmapping.ObjectMapper;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializerCollection;

public final class ScriptCompiler {

	private final ObjectMapper.Factory mapperFactory;

	public ScriptCompiler(ObjectMapper.Factory mapperFactory) {
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

		var mapper = mapperFactory.get(ScriptDto.class);
		var raw = mapper.load(patched);

		CompilerRegistry reg = new CompilerRegistry();
		reg.register(ScriptDto.class, new ScriptNodeCompiler());
		reg.register(PermissionsDto.class, new PermissionsCompiler());
		reg.register(DefaultsDto.class, new DefaultsCompiler());
		reg.register(TargetDto.class, new TargetCompiler(false));
		reg.register(TargetKindDto.class, new TargetKindCompiler(false));
		reg.register(TargetServerDto.class, new TargetServerCompiler());
		reg.register(ArgDto.ListWrapper.class, new ArgListCompiler());

		var sink = new CollectingProblemSink();
		var ctx = new CompileContext(reg, null);

		Script script = ctx.compile(ScriptDto.class, raw, sink, Path.root());
		return new Result(script, sink);
	}
}
