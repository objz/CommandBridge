package dev.objz.commandbridge.velocity;

import com.velocitypowered.api.proxy.ProxyServer;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.Argument;
import dev.objz.commandbridge.backends.net.out.ctx.InvokedCommandContext;
import dev.objz.commandbridge.cmd.CommandRegistryInterface;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.net.OutNode;
import dev.objz.commandbridge.net.proto.MessageType;
import dev.objz.commandbridge.net.payloads.cmd.CommandStub;
import dev.objz.commandbridge.scripting.model.records.mapping.ArgMapping;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

final class VelocityCommandRegistry implements CommandRegistryInterface {

    private final Set<String> registeredCommands = ConcurrentHashMap.newKeySet();
    private final Set<String> registeredAliases = ConcurrentHashMap.newKeySet();
    private final VelocityArgumentMapper argumentMapper;
    private final OutNode outNode;
    private final Object registrationLock = new Object();

    VelocityCommandRegistry(ProxyServer proxy, OutNode outNode) {
        this.argumentMapper = new VelocityArgumentMapper(proxy);
        this.outNode = outNode;
    }

    @Override
    public void register(CommandStub stub) throws Exception {
        String cmdName = stub.name();

        synchronized (registrationLock) {
            if (registeredCommands.contains(cmdName)) {
                Log.debug("Command '{}' is already registered, it will be replaced", cmdName);
            }

            CommandAPICommand cmd = new CommandAPICommand(cmdName);

            if (stub.description() != null && !stub.description().isBlank()) {
                cmd.withShortDescription(stub.description());
            }

            if (stub.aliases() != null && !stub.aliases().isEmpty()) {
                cmd.withAliases(stub.aliases().toArray(String[]::new));
                registeredAliases.addAll(stub.aliases());
            }

            if (stub.args() != null && !stub.args().isEmpty()) {
                List<Argument<?>> arguments = new ArrayList<>(stub.args().size());
                for (ArgMapping argMapping : stub.args()) {
                    Argument<?> argument = argumentMapper.map(argMapping);
                    if (!argMapping.required()) {
                        argument.setOptional(true);
                    }
                    arguments.add(argument);
                }
                cmd.withArguments(arguments);
            }

            cmd.executes((sender, args) -> {
                outNode.send(
                        MessageType.INVOKED_COMMAND,
                        new InvokedCommandContext(cmdName, sender, args, stub));
            });

            cmd.register();
            registeredCommands.add(cmdName);

            int argCount = stub.args() != null ? stub.args().size() : 0;
            int aliasCount = stub.aliases() != null ? stub.aliases().size() : 0;
            Log.debug("Registered command '{}' with {} arg(s) and {} alias(es)",
                    cmdName, argCount, aliasCount);
        }
    }

    @Override
    public void unregisterAll() throws Exception {
        synchronized (registrationLock) {
            if (registeredCommands.isEmpty()) {
                return;
            }

            Set<String> toUnregister = new HashSet<>(registeredCommands);
            Log.info("Deregistering '{}' {}", toUnregister.size(),
                    Log.plural(toUnregister.size(), "command", "commands"));

            toUnregister.addAll(registeredAliases);
            for (String cmdName : toUnregister) {
                try {
                    CommandAPI.unregister(cmdName, true);
                    Log.debug("Unregistered command '{}'", cmdName);
                } catch (Exception e) {
                    Log.warn("Failed to unregister command '{}': {}", cmdName, e.getMessage());
                }
            }

            registeredCommands.clear();
            registeredAliases.clear();
        }
    }
}
