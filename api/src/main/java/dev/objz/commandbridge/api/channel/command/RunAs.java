package dev.objz.commandbridge.api.channel.command;

/** Defines the execution context for a command. */
public enum RunAs {
    /** Run as the server console. */
    CONSOLE,
    /** Run as a specific player. */
    PLAYER,
    /** Run as a player with temporary operator permissions. */
    OPERATOR
}
