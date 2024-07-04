package org.commandbridge.utilities;

import org.bukkit.entity.Player;

public class StringParser {
    public static String parsePlaceholders(String command, Player player) {
        return command.replace("%player%", player.getName())
                .replace("%uuid%", player.getUniqueId().toString())
                .replace("%server%", player.getServer().getName());
    }
}
