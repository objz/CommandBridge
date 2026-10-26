package dev.objz.commandbridge.net.payloads.cmd;

import dev.objz.commandbridge.cmd.ref.EntityRef;
import dev.objz.commandbridge.cmd.ref.Location2D;
import dev.objz.commandbridge.cmd.ref.Location3D;
import dev.objz.commandbridge.net.proto.Envelope;
import dev.objz.commandbridge.scripting.model.enums.ArgType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class InvokedCommandTest {
    @Test
    void complexArgumentsKeepTheirTypesAcrossTheWire() throws Exception {
        var entity = new EntityRef("PLAYER", "00000000-0000-0000-0000-000000000001", "Alex");
        var invoked = new InvokedCommand("test", List.of(
                new InvokedCommand.TypedArgument(ArgType.PLAYERS, List.of(entity)),
                new InvokedCommand.TypedArgument(ArgType.ENTITIES, entity),
                new InvokedCommand.TypedArgument(ArgType.LOCATION, new Location3D("world", 1, 2, 3)),
                new InvokedCommand.TypedArgument(ArgType.LOCATION_2D, new Location2D("world", 4, 5)),
                new InvokedCommand.TypedArgument(ArgType.PLAYERS, null)),
                new SenderContext.Player("Alex", entity.uuid(), Set.of("commandbridge.command.test")));

        assertEquals(invoked, Envelope.MAPPER.readValue(Envelope.MAPPER.writeValueAsBytes(invoked), InvokedCommand.class));
    }

    @Test
    void legacySenderHasNoAssertedPermissions() throws Exception {
        var sender = Envelope.MAPPER.readValue("{\"type\":\"PLAYER\",\"name\":\"Alex\",\"uuid\":\"uuid\"}", SenderContext.class);
        assertEquals(new SenderContext.Player("Alex", "uuid", Set.of()), sender);
    }

    @Test
    void aPeerCannotSelectAnArbitraryJavaType() {
        assertThrows(Exception.class, () -> Envelope.MAPPER.readValue(
                "{\"type\":\"PLAYERS\",\"value\":{\"@class\":\"java.lang.Runtime\"}}", InvokedCommand.TypedArgument.class));
    }
}
