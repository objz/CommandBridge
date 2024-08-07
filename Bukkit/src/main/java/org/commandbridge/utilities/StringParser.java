package org.commandbridge.utilities;

import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.Objects;

public class StringParser {
    public static String parsePlaceholders(String command, Player player) {
        return command.replace("%player%", player.getName())
                .replace("%uuid%", player.getUniqueId().toString())
                .replace("%server%", player.getServer().getName())
                .replace("%world%", player.getWorld().getName());
    }
    public static String parseConsoleCommands(String command, ConsoleCommandSender consoleCommandSender) {
        return command.replace("%server%", consoleCommandSender.getServer().getName());
    }

    public static String parseBlockCommands(String command, BlockCommandSender blockCommandSender) {
        return command.replace("%server%", blockCommandSender.getServer().getName())
                .replace("%world%", blockCommandSender.getBlock().getWorld().getName())
                .replace("%player%", Objects.requireNonNull(blockCommandSender.getServer().getPlayer(blockCommandSender.getName())).getName())
                .replace("%uuid%", Objects.requireNonNull(blockCommandSender.getServer().getPlayer(blockCommandSender.getName())).getUniqueId().toString());
    }
}
