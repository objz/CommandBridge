package dev.objz.commandbridge.scripting;

import java.io.InputStream;

import dev.objz.commandbridge.scripting.bind.BindContext;
import dev.objz.commandbridge.scripting.bind.RecordBinder;
import dev.objz.commandbridge.scripting.bind.TypeAdapterRegistry;
import dev.objz.commandbridge.scripting.bind.adapters.DurationAdapter;
import dev.objz.commandbridge.scripting.bind.adapters.EnumAdapter;
import dev.objz.commandbridge.scripting.bind.adapters.ListAdapter;
import dev.objz.commandbridge.scripting.bind.adapters.PrimitivesAdapter;
import dev.objz.commandbridge.scripting.bind.adapters.RecordAdapter;
import dev.objz.commandbridge.scripting.bind.adapters.StringAdapter;
import dev.objz.commandbridge.scripting.validation.ProblemSink;
import dev.objz.commandbridge.scripting.yaml.YamlNode;
import dev.objz.commandbridge.scripting.yaml.YamlParser;
import org.snakeyaml.engine.v2.exceptions.Mark;
import org.snakeyaml.engine.v2.exceptions.MarkedYamlEngineException;

public final class ScriptLoader {

	public static final class LoadResult<T> {
		public final T value;
		public final ProblemSink problems;

		LoadResult(T value, ProblemSink problems) {
			this.value = value;
			this.problems = problems;
		}

		public boolean ok() {
			return !problems.hasErrors();
		}
	}

	private static final RecordBinder BINDER = new RecordBinder();
	private static final TypeAdapterRegistry REGISTRY = new TypeAdapterRegistry()
			.register(new PrimitivesAdapter())
			.register(new StringAdapter())
			.register(new EnumAdapter())
			.register(new DurationAdapter())
			.register(new ListAdapter())
			.register(new RecordAdapter(BINDER));

	private static BindContext newContext() {
		return new BindContext(REGISTRY, new ProblemSink());
	}

	public static <T> LoadResult<T> loadResult(Class<T> modelType, InputStream in) {
		var parser = new YamlParser();
		BindContext ctx = newContext();

		YamlNode node;
		try {
			node = parser.parse(in);
		} catch (Exception ex) {
			ctx.problems().error(null, prettyYamlError(ex));
			return new LoadResult<>(null, ctx.problems());
		}

		T result = BINDER.bindRecord(modelType, node, ctx);
		return new LoadResult<>(result, ctx.problems());
	}

	private static String prettyYamlError(Exception ex) {
		if (ex instanceof MarkedYamlEngineException mye) {
			Mark mark = mye.getProblemMark().orElse(null);
			if (mark != null) {
				int line = mark.getLine() + 1;
				int col = mark.getColumn() + 1;
				String problem = safeProblem(mye.getProblem());
				return "YAML error at line " + line + ", column " + col + ": " + problem;
			}
			return "YAML error: " + safeProblem(mye.getProblem());
		}
		return "YAML error: " + (ex.getMessage() != null ? ex.getMessage() : ex.toString());
	}

	private static String safeProblem(String s) {
		return (s == null || s.isBlank()) ? "parse error" : s;
	}
}
