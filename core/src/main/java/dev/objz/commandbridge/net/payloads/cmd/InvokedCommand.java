package dev.objz.commandbridge.net.payloads.cmd;

import java.util.List;

import dev.objz.commandbridge.scripting.model.enums.ArgType;

public record InvokedCommand(
        String name,
        List<TypedArgument> args,
        SenderContext sender) {

    public record TypedArgument(
            ArgType type,
            Object value) {
    }
}
