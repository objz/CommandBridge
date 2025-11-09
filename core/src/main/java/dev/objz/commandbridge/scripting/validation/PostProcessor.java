package dev.objz.commandbridge.scripting.validation;

import dev.objz.commandbridge.scripting.bind.BindContext;
import dev.objz.commandbridge.scripting.bind.RecordBinder;
//TODO: check if scripts are duplicte (same name)
//add player join listener for scheduling the command
public interface PostProcessor {
	void process(RecordBinder.MutableRecordBuffer buffer, BindContext ctx);
}
