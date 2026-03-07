package dev.objz.commandbridge.backends.platform.cmd;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.executors.CommandExecutor;
import dev.objz.commandbridge.backends.net.client.BackendClient;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.util.MM;
import net.kyori.adventure.audience.Audience;

import java.util.concurrent.CompletableFuture;

public final class ClientCommands {

    private ClientCommands() {
    }

    public static void register(BackendClient client) {
        new CommandAPICommand("commandbridgeclient")
                .withAliases("cbc")
                .withPermission("commandbridge.admin")
                .withSubcommand(new CommandAPICommand("reconnect")
                        .executes((CommandExecutor) (sender, args) -> {
                            Audience src = (Audience) sender;

                            MM.msg()
                                    .space()
                                    .line(MM.accent("Initiating reconnection sequence"))
                                    .line(MM.muted("Closing existing connection"))
                                    .send(src);

                            CompletableFuture.runAsync(() -> {
                                try {
                                    client.reconnect();

                                    Thread.sleep(500);

                                    var status = client.status();

                                    MM.msg()
                                            .space()
                                            .line(MM.ok("Reconnection attempt completed"))
                                            .kv("Status", status.name())
                                            .line(MM.muted("Check console for full handshake details"))
                                            .send(src);

                                } catch (Exception e) {
                                    Log.error(e, "Failed to reconnect via command");
                                    MM.msg()
                                            .space()
                                            .line(MM.error("Reconnection failed"))
                                            .line(MM.muted(e.getMessage()))
                                            .send(src);
                                }
                            });
                        }))
                .register();
    }
}
