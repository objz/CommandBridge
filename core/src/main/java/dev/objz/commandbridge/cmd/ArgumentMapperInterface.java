package dev.objz.commandbridge.cmd;

import dev.objz.commandbridge.scripting.model.records.mapping.ArgMapping;

public interface ArgumentMapperInterface<A> {
    A map(ArgMapping mapping) throws Exception;
}
