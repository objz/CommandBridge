package dev.objz.commandbridge.net.payloads.cmd;

import java.util.List;

import dev.objz.commandbridge.scripting.model.records.mapping.ArgMapping;


public record CommandStub(
        String name,          
        List<String> aliases,  
        String description,
	List<ArgMapping> args
) {}
