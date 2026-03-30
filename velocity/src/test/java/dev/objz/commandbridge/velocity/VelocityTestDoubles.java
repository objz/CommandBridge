package dev.objz.commandbridge.velocity;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.permission.PermissionSubject;
import com.velocitypowered.api.permission.Tristate;
import dev.objz.commandbridge.api.channel.command.RunAs;
import dev.objz.commandbridge.net.payloads.cmd.InvokedCommand;
import dev.objz.commandbridge.net.payloads.cmd.SenderContext;
import dev.objz.commandbridge.scripting.model.Defaults;
import dev.objz.commandbridge.scripting.model.Permissions;
import dev.objz.commandbridge.scripting.model.Script;
import dev.objz.commandbridge.scripting.model.records.mapping.CmdMapping;
import dev.objz.commandbridge.scripting.model.records.mapping.IdMapping;
import dev.objz.commandbridge.velocity.dispatch.model.ExecutionContext;
import net.kyori.adventure.text.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class VelocityTestDoubles {

    public static class StubCommandSource implements CommandSource, PermissionSubject {
        private final Map<String, Boolean> permissions;

        public StubCommandSource(Map<String, Boolean> permissions) {
            this.permissions = new HashMap<>(permissions);
        }

        @Override
        public boolean hasPermission(String permission) {
            return permissions.getOrDefault(permission, false);
        }

        @Override
        public Tristate getPermissionValue(String permission) {
            Boolean value = permissions.get(permission);
            if (value == null) {
                return Tristate.UNDEFINED;
            }
            return value ? Tristate.TRUE : Tristate.FALSE;
        }

        @Override
        public void sendMessage(Component message) {
        }
    }

    public static class StubPlayer extends StubCommandSource {
        private final UUID uuid;
        private final String username;

        public StubPlayer(UUID uuid, String username, Map<String, Boolean> permissions) {
            super(permissions);
            this.uuid = uuid;
            this.username = username;
        }

        public UUID getUniqueId() {
            return uuid;
        }

        public String getUsername() {
            return username;
        }
    }

    public static Script script(String name) {
        return new Script(
                4,
                name,
                true,
                null,
                List.of(),
                new Permissions(true, false),
                List.of(new IdMapping("test", null)),
                new Defaults(RunAs.CONSOLE, null, null, null, null),
                List.of(),
                List.of(new CmdMapping("say test", null, null, null, null, null))
        );
    }

    public static ExecutionContext context(Script script, CommandSource source, String[] invokedArgs) {
        InvokedCommand invoked = new InvokedCommand(
                "test",
                List.of(),
                new SenderContext.Console()
        );
        return new ExecutionContext(
                invoked,
                null,
                source,
                null,
                script,
                Map.of(),
                null,
                -1
        );
    }

    private VelocityTestDoubles() {
    }
}
