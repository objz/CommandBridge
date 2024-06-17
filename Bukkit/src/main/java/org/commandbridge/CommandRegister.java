package org.commandbridge;



import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;

import java.lang.reflect.Field;

public class CommandRegister {
    private final CommandBridge plugin;


    public CommandRegister(CommandBridge plugin) {
        this.plugin = plugin;
    }

    public void registerCommand(String command) {
        VerboseLogger logger = new VerboseLogger(plugin);
        try {
            Field comandMapField =plugin.getServer().getClass().getDeclaredField("commandMap");
            comandMapField.setAccessible(true);
            CommandMap commandMap = (CommandMap) comandMapField.get(plugin.getServer());

            Command newCommand = new BukkitCommand(command) {
                @Override
                public boolean execute(CommandSender sender, String commandLabel, String[] args) {
                    sender.sendMessage("Command executed");
                    return false;
                }
            };
            commandMap.register(plugin.getName(), newCommand);
        } catch (Exception e) {
            logger.error("Failed to register command: " + command, e);
        }
    }

}