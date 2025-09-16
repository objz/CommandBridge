package dev.objz.commandbridge.main.proto.cmd;

import java.util.List;

public record CommandStub(
        String name,              // script name (unique)
        List<String> aliases,   // extra aliases
        String description,     // short help text
        String usage            // e.g. "/eco <player> <amount> <serverId>"
) {}
