package dev.objz.commandbridge.scripting.validation;

import dev.objz.commandbridge.scripting.bind.BindContext;
import dev.objz.commandbridge.scripting.bind.RecordBinder;

public interface PostProcessor {
	void process(RecordBinder.MutableRecordBuffer buffer, BindContext ctx);
}
