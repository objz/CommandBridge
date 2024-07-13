package org.commandbridge.utilities;

import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

public class StringParser {
    public static String parsePlaceholders(String command, Player player) {
        return command.replace("%player%", player.getName())
                .replace("%uuid%", player.getUniqueId().toString())
                .replace("%server%", player.getServer().getName());
    }
    public static String parseConsoleCommands(String command, ConsoleCommandSender consoleCommandSender) {
        return command.replace("%server%", consoleCommandSender.getServer().getName());}
}
