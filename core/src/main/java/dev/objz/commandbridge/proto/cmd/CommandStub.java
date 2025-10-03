package dev.objz.commandbridge.proto.cmd;

import java.util.List;


public record CommandStub(
        String name,          
        List<String> aliases,  
        String description
	// List<ArgDef> args
) {}
