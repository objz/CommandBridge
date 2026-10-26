package dev.objz.commandbridge.net.payloads.cmd;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

import dev.objz.commandbridge.cmd.ref.Location3D;
import java.util.Set;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = SenderContext.Player.class, name = "PLAYER"),
        @JsonSubTypes.Type(value = SenderContext.Console.class, name = "CONSOLE"),
        @JsonSubTypes.Type(value = SenderContext.Block.class, name = "BLOCK"),
        @JsonSubTypes.Type(value = SenderContext.Other.class, name = "OTHER")
})
public sealed interface SenderContext permits
        SenderContext.Player, SenderContext.Console, SenderContext.Block, SenderContext.Other {

    @JsonTypeName("PLAYER")
    record Player(String name, String uuid, Set<String> permissions) implements SenderContext {
        public Player {
            permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
        }

        public Player(String name, String uuid) {
            this(name, uuid, Set.of());
        }
    }

    @JsonTypeName("CONSOLE")
    record Console() implements SenderContext {
    }

    @JsonTypeName("BLOCK")
    record Block(Location3D location) implements SenderContext {
    }

    @JsonTypeName("OTHER")
    record Other(String className) implements SenderContext {
    }

}
