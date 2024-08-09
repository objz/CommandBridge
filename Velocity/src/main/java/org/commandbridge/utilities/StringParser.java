package org.commandbridge.utilities;

import com.velocitypowered.api.proxy.Player;


public class StringParser {
    public static String parsePlaceholders(String command, Player player, String[] args) {
        String serverName = player.getCurrentServer()
                               .map(server -> server.getServerInfo().getName())
                               .orElse("defaultServeName");

        return command.replace("%player%", player.getUsername())
                      .replace("%uuid%", player.getUniqueId().toString())
                      .replace("%server%", serverName)
                      .replace("%args%", appendArguments(args));
    }

    private static String appendArguments(String[] args) {
    if (args == null || args.length == 0) {
        return "";
    }
    return String.join(" ", args);
    }
}