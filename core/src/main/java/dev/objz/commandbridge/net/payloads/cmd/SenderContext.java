package dev.objz.commandbridge.net.payloads.cmd;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

import dev.objz.commandbridge.cmd.ref.Location3D;

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
    record Player(String name, String uuid) implements SenderContext {
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
