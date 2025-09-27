package dev.objz.commandbridge.main.scripting.v3.loader;

import dev.objz.commandbridge.main.scripting.v3.model.args.ArgDef;
import dev.objz.commandbridge.main.scripting.v3.model.args.ChoiceArgDef;
import dev.objz.commandbridge.main.scripting.v3.model.args.SimpleArgDef;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.lang.reflect.*;
import java.util.*;

public final class PathIntrospector {
	private final Set<Class<?>> simple = Set.of(
			String.class, Integer.class, Long.class, Boolean.class,
			Double.class, Float.class);

	public Set<String> allowedPaths(Class<?> root) {
		Set<String> out = new LinkedHashSet<>();
		Set<Type> seen = new HashSet<>();
		walk(root, "", out, seen);
		return out;
	}

	private void walk(Type type, String base, Set<String> out, Set<Type> seen) {
		if (seen.contains(type))
			return;
		seen.add(type);
		Class<?> cls = toClass(type);
		if (cls == null)
			return;
		if (cls.isEnum() || simple.contains(cls)) {
			if (!base.isEmpty())
				out.add(base);
			return;
		}

		for (Field f : cls.getDeclaredFields()) {
			if (Modifier.isStatic(f.getModifiers()) || f.isSynthetic())
				continue;
			String key = settingName(f);
			String path = base.isEmpty() ? key : base + "." + key;
			Type ft = f.getGenericType();

			if (isList(ft)) {
				out.add(path); // container
				Type elem = ((ParameterizedType) ft).getActualTypeArguments()[0];
				String arr = path + "[]";
				out.add(arr);
				if (toClass(elem) != null && ArgDef.class.isAssignableFrom(toClass(elem))) {
					walk(SimpleArgDef.class, arr, out, seen);
					walk(ChoiceArgDef.class, arr, out, seen);
				} else {
					walk(elem, arr, out, seen);
				}
			} else if (isSimple(ft)) {
				out.add(path);
			} else if (toClass(ft).isEnum()) {
				out.add(path);
			} else {
				out.add(path);
				walk(ft, path, out, seen);
			}
		}
	}

	private static boolean isList(Type t) {
		if (t instanceof ParameterizedType pt) {
			return List.class.isAssignableFrom((Class<?>) pt.getRawType());
		}
		return false;
	}

	private boolean isSimple(Type t) {
		Class<?> c = toClass(t);
		return c != null && (c.isEnum() || simple.contains(c));
	}

	private static Class<?> toClass(Type t) {
		if (t instanceof Class<?> c)
			return c;
		if (t instanceof ParameterizedType p)
			return (Class<?>) p.getRawType();
		return null;
	}

	private static String settingName(Field f) {
		Setting s = f.getAnnotation(Setting.class);
		if (s != null && !s.value().isBlank())
			return s.value();
		return f.getName();
	}
}
