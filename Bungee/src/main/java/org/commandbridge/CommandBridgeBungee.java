package org.commandbridge;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.api.plugin.Event;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

public class CommandBridgeBungee extends Plugin implements Listener {

  private final Set<String> registeredCommands = new HashSet<>();
  private VerboseLogger verboseLogger;

  @Override
  public void onEnable() {
    verboseLogger = new VerboseLogger(this, getLogger());
    getProxy().registerChannel("commandbridge:main");
    getProxy().getPluginManager().registerListener(this, this);
    loadConfig();
    registerCommands();
    verboseLogger.info("CommandBridge has been enabled!");
  }

  @Override
  public void onDisable() {
    getProxy().unregisterChannel("commandbridge:main");
    getProxy().getPluginManager().unregisterListeners(this);
    verboseLogger.info("CommandBridge has been disabled!");
  }

  private void loadConfig() {
    // Implement configuration loading logic here
  }

  private void registerCommands() {
    // Register commands logic here
  }

  public VerboseLogger getVerboseLogger() {
    return verboseLogger;
  }

  public Set<String> getRegisteredCommands() {
    return registeredCommands;
  }

  public void addRegisteredCommand(String command) {
    registeredCommands.add(command);
  }

  public void clearRegisteredCommands() {
    registeredCommands.clear();
  }

  @EventHandler
  public void onPluginMessage(PluginMessageEvent event) {
    // Handle plugin messages here
  }


  public boolean isVerboseOutputEnabled() {
    return true; // Implement verbose output logic here
  }
}
