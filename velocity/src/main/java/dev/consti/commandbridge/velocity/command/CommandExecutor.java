package dev.consti.commandbridge.velocity.command;

import com.velocitypowered.api.proxy.ProxyServer;

import dev.consti.commandbridge.velocity.Main;
import dev.consti.commandbridge.velocity.core.Runtime;
import dev.consti.commandbridge.velocity.utils.ProxyUtils;
import dev.consti.foundationlib.json.MessageParser;
import dev.consti.foundationlib.logging.Logger;

public class CommandExecutor {
    private final ProxyServer proxy;
    private final Main plugin;
    private final Logger logger;

    public CommandExecutor() {
        this.plugin = Main.getInstance();
        this.proxy = ProxyUtils.getProxyServer();
        this.logger = Runtime.getInstance().getLogger();
    }
    
    public void dispatchCommand(String message) {
        MessageParser parser = new MessageParser(message);

        proxy.getCommandManager().executeAsync(proxy.getConsoleCommandSource(), parser.getBodyValueAsString("command"));


    }

}
