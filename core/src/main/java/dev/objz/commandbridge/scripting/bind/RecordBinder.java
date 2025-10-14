package dev.objz.commandbridge.scripting.bind;

import dev.objz.commandbridge.scripting.anno.Default;
import dev.objz.commandbridge.scripting.anno.Model;
import dev.objz.commandbridge.scripting.anno.ModelRoot;
import dev.objz.commandbridge.scripting.anno.Required;
import dev.objz.commandbridge.scripting.anno.YmlKey;
import dev.objz.commandbridge.scripting.validation.PostProcessor;
import dev.objz.commandbridge.scripting.validation.ProblemSink;
import dev.objz.commandbridge.scripting.validation.processor.ArgumentOrderProcessor;
import dev.objz.commandbridge.scripting.validation.processor.DefaultProcessor;
import dev.objz.commandbridge.scripting.validation.processor.MaxProcessor;
import dev.objz.commandbridge.scripting.validation.processor.MergeProcessor;
import dev.objz.commandbridge.scripting.validation.processor.MinProcessor;
import dev.objz.commandbridge.scripting.validation.processor.PatternProcessor;
import dev.objz.commandbridge.scripting.validation.processor.PlatformProcessor;
import dev.objz.commandbridge.scripting.validation.processor.RequiredProcessor;
import dev.objz.commandbridge.scripting.validation.processor.ResolvableProcessor;
import dev.objz.commandbridge.scripting.yaml.YamlNode;

import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.*;

public final class RecordBinder {

	private static final ThreadLocal<String> PATH = new ThreadLocal<>();

	public static String currentPath() {
		return PATH.get();
	}

	public static void setCurrentPath(String p) {
		if (p == null || p.isBlank())
			PATH.remove();
		else
			PATH.set(p);
	}

	private final List<PostProcessor> postProcessors;

	public RecordBinder() {
		this.postProcessors = List.of(
				new DefaultProcessor(),
				new RequiredProcessor(),
				new PatternProcessor(),
				new MinProcessor(),
				new MaxProcessor(),
				new ResolvableProcessor(),
				new MergeProcessor(),
				new PlatformProcessor(),
				new ArgumentOrderProcessor()

		);
	}

	public <T> T bindRecord(Class<T> recordType, YamlNode node, BindContext ctx) {
		if (!(node instanceof YamlNode.Mapping mapping)) {
			String path = rootLabelOf(recordType);
			if (path == null)
				path = sectionNameOf(recordType);
			ctx.problems().error(path, "expected a mapping/object");
			return constructWithDefaults(recordType, ctx, path);
		}
		String rootLabel = rootLabelOf(recordType);
		boolean suppressRootForChildren = (rootLabel != null);
		return bindFromMapping(recordType, mapping, ctx,
				(rootLabel != null ? rootLabel : sectionNameOf(recordType)), suppressRootForChildren);
	}

	public <T> T bindRecord(Class<T> recordType, YamlNode node, BindContext ctx, String sectionOverride) {
		String effective = (sectionOverride != null) ? sectionOverride : currentPath();
		if (!(node instanceof YamlNode.Mapping mapping)) {
			String path = (effective != null) ? effective : sectionNameOf(recordType);
			ctx.problems().error(path, "expected a mapping/object");
			return constructWithDefaults(recordType, ctx, path);
		}
		return bindFromMapping(recordType, mapping, ctx,
				(effective != null ? effective : sectionNameOf(recordType)), false);
	}

	private <T> T bindFromMapping(Class<T> recordType,
			YamlNode.Mapping mapping,
			BindContext ctx,
			String sectionNameForDiagnostics,
			boolean suppressRootForChildren) {
		var comps = recordType.getRecordComponents();
		Object[] values = new Object[comps.length];

		for (int i = 0; i < comps.length; i++) {
			RecordComponent c = comps[i];
			String key = keyOf(c);
			YamlNode child = mapping.entries().get(key);

			final String childPath;
			if (suppressRootForChildren) {
				childPath = c.getName();
			} else if (sectionNameForDiagnostics != null && !sectionNameForDiagnostics.isBlank()) {
				childPath = sectionNameForDiagnostics + "." + c.getName();
			} else {
				childPath = sectionNameOf(recordType) + "." + c.getName();
			}

			values[i] = convertChild(c.getGenericType(), child, ctx, childPath);
		}

		MutableRecordBuffer buffer = new MutableRecordBuffer(recordType, sectionNameForDiagnostics, comps,
				values);
		for (PostProcessor p : postProcessors) {
			p.process(buffer, ctx);
		}

		return construct(recordType, comps, buffer.values(), ctx.problems());
	}

	private Object convertChild(Type targetType, YamlNode child, BindContext ctx, String path) {
		if (child == null)
			return null;

		if (isRecordType(targetType)) {
			if (!(child instanceof YamlNode.Mapping)) {
				ctx.problems().error(path, "expected a mapping/object");
				return null;
			}
			try {
				return bindRecord((Class<?>) targetType, child, ctx, path);
			} catch (Exception ex) {
				ctx.problems().error(path, "invalid value: " + ex.getMessage());
				return null;
			}
		} else if (isListType(targetType)) {
			if (!(child instanceof YamlNode.Sequence)) {
				ctx.problems().error(path, "expected a list");
				return null;
			}
			String prev = currentPath();
			try {
				setCurrentPath(path);
				return ctx.adapters().find(targetType).fromYaml(child, targetType, ctx);
			} finally {
				setCurrentPath(prev);
			}
		} else if (isMapType(targetType)) {
			if (!(child instanceof YamlNode.Mapping)) {
				ctx.problems().error(path, "expected a mapping/object");
				return null;
			}
		} else {
			if (!(child instanceof YamlNode.Scalar)) {
				ctx.problems().error(path, "expected a value");
				return null;
			}
		}

		try {
			return ctx.adapters().find(targetType).fromYaml(child, targetType, ctx);
		} catch (Exception ex) {
			ctx.problems().error(path, "invalid value: " + ex.getMessage());
			return null;
		}
	}

	private <T> T constructWithDefaults(Class<T> recordType, BindContext ctx, String sectionNameForDiagnostics) {
		var comps = recordType.getRecordComponents();
		Object[] values = new Object[comps.length];
		MutableRecordBuffer buffer = new MutableRecordBuffer(recordType, sectionNameForDiagnostics, comps,
				values);
		new DefaultProcessor().process(buffer, ctx);
		return construct(recordType, comps, buffer.values(), ctx.problems());
	}

	private <T> T construct(Class<T> recordType, RecordComponent[] comps, Object[] values, ProblemSink problems) {
		try {
			Class<?>[] argTypes = Arrays.stream(comps).map(RecordComponent::getType).toArray(Class[]::new);
			Constructor<T> ctor = recordType.getDeclaredConstructor(argTypes);
			ctor.setAccessible(true);
			return ctor.newInstance(values);
		} catch (ReflectiveOperationException e) {
			problems.error(pathOf(recordType), "could not construct model: " + e.getMessage());
			return null;
		}
	}

	private static String keyOf(RecordComponent c) {
		YmlKey ann = c.getAnnotation(YmlKey.class);
		return ann != null ? ann.value() : c.getName();
	}

	private static String sectionNameOf(Class<?> type) {
		Model m = type.getAnnotation(Model.class);
		if (m != null && !m.value().isBlank())
			return m.value();
		ModelRoot mr = type.getAnnotation(ModelRoot.class);
		if (mr != null && !mr.value().isBlank())
			return mr.value();
		return type.getSimpleName();
	}

	private static String rootLabelOf(Class<?> type) {
		ModelRoot mr = type.getAnnotation(ModelRoot.class);
		return (mr != null && !mr.value().isBlank()) ? mr.value() : null;
	}

	private static String pathOf(Class<?> type) {
		return sectionNameOf(type);
	}

	private static boolean isRecordType(Type t) {
		return (t instanceof Class<?> c) && c.isRecord();
	}

	private static boolean isListType(Type t) {
		if (t instanceof Class<?> c)
			return List.class.isAssignableFrom(c);
		if (t instanceof ParameterizedType p)
			return (p.getRawType() instanceof Class<?> rc) && List.class.isAssignableFrom(rc);
		return false;
	}

	private static boolean isMapType(Type t) {
		if (t instanceof Class<?> c)
			return Map.class.isAssignableFrom(c);
		if (t instanceof ParameterizedType p)
			return (p.getRawType() instanceof Class<?> rc) && Map.class.isAssignableFrom(rc);
		return false;
	}

	public static final class MutableRecordBuffer {
		private final Class<?> recordClass;
		private final String sectionName;
		private final RecordComponent[] components;
		private final Object[] values;

		public MutableRecordBuffer(Class<?> recordClass, String sectionNameForDiagnostics,
				RecordComponent[] components, Object[] values) {
			this.recordClass = recordClass;
			this.components = components;
			this.values = values;
			this.sectionName = (sectionNameForDiagnostics != null && !sectionNameForDiagnostics.isBlank())
					? sectionNameForDiagnostics
					: sectionNameOf(recordClass);
		}

		public Class<?> recordClass() {
			return recordClass;
		}

		public RecordComponent[] components() {
			return components;
		}

		public Object[] values() {
			return values;
		}

		public Object get(int idx) {
			return values[idx];
		}

		public void set(int idx, Object value) {
			values[idx] = value;
		}

		public Optional<Default> defaultOf(int idx) {
			return Optional.ofNullable(components[idx].getAnnotation(Default.class));
		}

		public Optional<Required> requiredOf(int idx) {
			return Optional.ofNullable(components[idx].getAnnotation(Required.class));
		}

		public String pathOf(int idx) {
			return sectionName + "." + components[idx].getName();
		}

		public Type typeOf(int idx) {
			return components[idx].getGenericType();
		}
	}
}
