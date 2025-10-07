package dev.objz.commandbridge.scripting.validation.processor;

import dev.objz.commandbridge.scripting.anno.Merge;
import dev.objz.commandbridge.scripting.bind.BindContext;
import dev.objz.commandbridge.scripting.bind.RecordBinder;
import dev.objz.commandbridge.scripting.model.Script;
import dev.objz.commandbridge.scripting.model.Defaults;
import dev.objz.commandbridge.scripting.model.records.mapping.CmdMapping;
import dev.objz.commandbridge.scripting.validation.PostProcessor;

import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public final class MergeProcessor implements PostProcessor {

	@Override
	public void process(RecordBinder.MutableRecordBuffer buf, BindContext ctx) {
		if (!Script.class.equals(buf.recordClass()))
			return;

		int idxDefaults = -1, idxCommands = -1;
		RecordComponent[] scriptComps = buf.components();
		for (int i = 0; i < scriptComps.length; i++) {
			String n = scriptComps[i].getName();
			if ("defaults".equals(n))
				idxDefaults = i;
			else if ("commands".equals(n))
				idxCommands = i;
		}
		if (idxDefaults < 0 || idxCommands < 0)
			return;

		Defaults defaults = (Defaults) buf.get(idxDefaults);
		List<CmdMapping> commands = (List<CmdMapping>) buf.get(idxCommands);

		if (defaults == null || commands == null || commands.isEmpty())
			return;

		Map<String, Object> defaultsByName = toComponentMap(defaults);

		RecordComponent[] cmdComps = CmdMapping.class.getRecordComponents();
		boolean[] mergeHere = new boolean[cmdComps.length];
		String[] mergeFrom = new String[cmdComps.length];

		for (int i = 0; i < cmdComps.length; i++) {
			Merge m = cmdComps[i].getAnnotation(Merge.class);
			if (m != null) {
				mergeHere[i] = true;
				mergeFrom[i] = (m.from() == null || m.from().isBlank()) ? "defaults" : m.from();
			}
		}

		List<CmdMapping> merged = new ArrayList<>(commands.size());
		for (CmdMapping c : commands) {
			if (c == null) {
				merged.add(null);
				continue;
			}
			Object[] values = deconstruct(c, cmdComps.length);

			for (int i = 0; i < cmdComps.length; i++) {
				if (!mergeHere[i])
					continue;
				if (values[i] != null)
					continue;

				if (!"defaults".equals(mergeFrom[i]))
					continue;

				String fieldName = cmdComps[i].getName();
				if (defaultsByName.containsKey(fieldName)) {
					values[i] = defaultsByName.get(fieldName);
				}
			}

			merged.add(construct(CmdMapping.class, cmdComps, values, ctx));
		}

		buf.set(idxCommands, List.copyOf(merged));
	}

	private static Map<String, Object> toComponentMap(Object record) {
		Map<String, Object> out = new HashMap<>();
		RecordComponent[] comps = record.getClass().getRecordComponents();
		for (RecordComponent rc : comps) {
			try {
				out.put(rc.getName(), rc.getAccessor().invoke(record));
			} catch (Throwable t) {
			}
		}
		return out;
	}

	private static Object[] deconstruct(Object record, int len) {
		Object[] arr = new Object[len];
		RecordComponent[] comps = record.getClass().getRecordComponents();
		for (int i = 0; i < comps.length; i++) {
			try {
				arr[i] = comps[i].getAccessor().invoke(record);
			} catch (Throwable t) {
				arr[i] = null;
			}
		}
		return arr;
	}

	private static <T> T construct(Class<T> type, RecordComponent[] comps, Object[] values, BindContext ctx) {
		try {
			Class<?>[] ptypes = new Class<?>[comps.length];
			for (int i = 0; i < comps.length; i++)
				ptypes[i] = comps[i].getType();
			var ctor = type.getDeclaredConstructor(ptypes);
			return type.cast(ctor.newInstance(values));
		} catch (Throwable t) {
			ctx.problems().error("commands", "internal merge failure: " + t.getMessage());
			return null;
		}
	}
}
