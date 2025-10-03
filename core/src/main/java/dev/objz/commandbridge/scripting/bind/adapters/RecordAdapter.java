package dev.objz.commandbridge.scripting.bind.adapters;

import java.lang.reflect.Type;

import dev.objz.commandbridge.scripting.bind.BindContext;
import dev.objz.commandbridge.scripting.bind.ConvertContext;
import dev.objz.commandbridge.scripting.bind.RecordBinder;
import dev.objz.commandbridge.scripting.bind.TypeAdapter;
import dev.objz.commandbridge.scripting.yaml.YamlNode;

public final class RecordAdapter implements TypeAdapter<Object> {

	private final RecordBinder binder;

	public RecordAdapter(RecordBinder binder) {
		this.binder = binder;
	}

	@Override
	public boolean supports(Type targetType) {
		return targetType instanceof Class<?> c && c.isRecord();
	}

	@Override
	public Object fromYaml(YamlNode node, Type targetType, ConvertContext ctx) {
		if (!(targetType instanceof Class<?> c) || !c.isRecord()) {
			throw new IllegalArgumentException("Target is not a record");
		}
		return binder.bindRecord(c, node, (BindContext) ctx);
	}

	@Override
	public YamlNode toYaml(Object value, Type targetType, ConvertContext ctx) {
		throw new UnsupportedOperationException("Record toYaml not implemented in this minimal sample");
	}
}
