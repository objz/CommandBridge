package dev.objz.commandbridge.paper.utils;

import java.lang.reflect.Field;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;

import dev.objz.commandbridge.paper.core.Runtime;

public class CommandUtils {
    private static CommandMap getCommandMap() {
        try {
            Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            return (CommandMap) commandMapField.get(Bukkit.getServer());
        } catch (Exception e) {
            Runtime.getInstance().getLogger().error("Error while accessing commandMap: {}", e);
            return null;
        }
    }

    public static boolean isCommandValid(String command) {
        String baseCommand = command.split(" ")[0];
        PluginCommand pluginCommand = Bukkit.getPluginCommand(baseCommand);
        if (pluginCommand != null) {
            return false;
        }

        CommandMap commandMap = getCommandMap();
        if (commandMap == null) {
            return false;
        }

        Command cmd = commandMap.getCommand(baseCommand);
        return cmd == null;
    }


}
