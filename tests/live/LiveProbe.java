package live;

import com.google.inject.Inject;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.permission.PermissionsSetupEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.objz.commandbridge.api.CommandBridgeProvider;
import dev.objz.commandbridge.api.channel.command.CommandPayload;
import dev.objz.commandbridge.api.channel.command.RunAs;
import dev.objz.commandbridge.api.platform.Platform;
import java.util.List;
import org.slf4j.Logger;

/** Test-only, non-destructive console marker and public API probe. */
public final class LiveProbe implements SimpleCommand {
    private final ProxyServer proxy;
    private final Logger log;

    @Inject
    public LiveProbe(ProxyServer proxy, Logger log) {
        this.proxy = proxy;
        this.log = log;
    }

    @Subscribe
    public void initialize(ProxyInitializeEvent event) {
        proxy.getCommandManager().register("liveprobe", this);
        log.info("LIVE_PROBE_READY");
    }

    @Subscribe
    public void permissions(PermissionsSetupEvent event) {
        if (event.getSubject() instanceof Player player) {
            event.setProvider(subject -> permission ->
                    permission.startsWith("commandbridge.command.")
                            ? (player.getUsername().equals("CBAllowed") ? Tristate.TRUE : Tristate.FALSE)
                            : Tristate.UNDEFINED);
        }
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return !(invocation.source() instanceof Player);
    }

    @Override
    public void execute(Invocation invocation) {
        String[] args = invocation.arguments();
        try {
            if (args.length == 2 && args[0].equals("mark") && args[1].matches("CBLIVE_[A-Za-z0-9_]+")) {
                log.info("LIVE_MARK {} sender=console", args[1]);
            } else if (args.length == 1 && args[0].equals("status")) {
                var api = CommandBridgeProvider.get();
                log.info("LIVE_STATUS id={} active={} servers={}", api.server().id(),
                        api.connectionState().isActive(),
                        api.connectedServers().map(ids -> ids.stream().sorted().toList()).orElse(List.of()));
            } else if (args.length == 3 && args[0].equals("api")
                    && args[2].matches("CBLIVE_[A-Za-z0-9_]+")) {
                var target = args[1].equals("paper") ? Platform.backend("paper") : Platform.velocity("secondary");
                String command = args[1].equals("paper") ? "say " + args[2] : "liveprobe mark " + args[2];
                CommandBridgeProvider.get().channel(CommandPayload.class).to(List.of(target))
                        .send(new CommandPayload(command, RunAs.CONSOLE))
                        .whenComplete((ignored, error) -> log.info("LIVE_API_DISPATCH {} ok={}", args[2], error == null));
            } else {
                log.error("LIVE_PROBE_BAD_ARGUMENTS");
            }
        } catch (Exception error) {
            log.error("LIVE_PROBE_ERROR {}", error.getClass().getSimpleName());
        }
    }
}
