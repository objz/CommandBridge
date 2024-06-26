package org.commandbridge.utilities;

import com.velocitypowered.api.proxy.Player;

public class StringParser {
    public static String parsePlaceholders(String command, Player player) {
        return command.replace("%player%", player.getUsername())
                .replace("%uuid%", player.getUniqueId().toString())
                .replace("%server%", player.getCurrentServer().get().getServerInfo().getName());
    }
}
