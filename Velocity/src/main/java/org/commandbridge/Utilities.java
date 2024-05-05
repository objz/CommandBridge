package org.commandbridge;

import com.velocitypowered.api.proxy.Player;

public class Utilities {
    public static String parsePlaceholders(String command, Player player) {
        return command.replace("%player%", player.getUsername())
                .replace("%uuid%", player.getUniqueId().toString())
                .replace("%server%", player.getCurrentServer().get().getServerInfo().getName());
    }
}
