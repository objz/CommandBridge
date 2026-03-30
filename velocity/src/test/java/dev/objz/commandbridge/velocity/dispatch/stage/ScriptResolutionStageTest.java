package dev.objz.commandbridge.velocity.dispatch.stage;

import dev.objz.commandbridge.api.channel.command.RunAs;
import dev.objz.commandbridge.net.payloads.cmd.InvokedCommand;
import dev.objz.commandbridge.net.payloads.cmd.SenderContext;
import dev.objz.commandbridge.scripting.model.Defaults;
import dev.objz.commandbridge.scripting.model.Permissions;
import dev.objz.commandbridge.scripting.model.Script;
import dev.objz.commandbridge.scripting.model.records.mapping.CmdMapping;
import dev.objz.commandbridge.scripting.model.records.mapping.IdMapping;
import dev.objz.commandbridge.velocity.ScriptManager;
import dev.objz.commandbridge.velocity.TestFixtures;
import dev.objz.commandbridge.velocity.VelocityTestDoubles;
import dev.objz.commandbridge.velocity.dispatch.model.ExecutionContext;
import dev.objz.commandbridge.velocity.dispatch.model.ExecutionResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for {@link ScriptResolutionStage} — verifies script lookup by exact name,
 * alias, and case-insensitive matching.
 */
final class ScriptResolutionStageTest {

    @BeforeAll
    static void setUp() {
        TestFixtures.ensureLog();
    }

    @Test
    void processExactNameMatchContinues() {
        Script lobby = scriptWithAliases("lobby", List.of());
        ScriptResolutionStage stage = new ScriptResolutionStage(managerWith(List.of(lobby)));
        ExecutionContext ctx = contextWithName("lobby");

        var result = new AtomicReference<ExecutionResult>();
        stage.process(ctx, result::set);

        var cont = assertInstanceOf(ExecutionResult.Continue.class, result.get());
        assertEquals("lobby", cont.context().script().name());
    }

    @Test
    void processAliasMatchContinues() {
        Script lobby = scriptWithAliases("lobby", List.of("hub", "spawn"));
        ScriptResolutionStage stage = new ScriptResolutionStage(managerWith(List.of(lobby)));
        ExecutionContext ctx = contextWithName("hub");

        var result = new AtomicReference<ExecutionResult>();
        stage.process(ctx, result::set);

        var cont = assertInstanceOf(ExecutionResult.Continue.class, result.get());
        assertEquals("lobby", cont.context().script().name());
    }

    @Test
    void processCaseInsensitiveMatchContinues() {
        Script lobby = scriptWithAliases("lobby", List.of());
        ScriptResolutionStage stage = new ScriptResolutionStage(managerWith(List.of(lobby)));
        ExecutionContext ctx = contextWithName("LOBBY");

        var result = new AtomicReference<ExecutionResult>();
        stage.process(ctx, result::set);

        var cont = assertInstanceOf(ExecutionResult.Continue.class, result.get());
        assertEquals("lobby", cont.context().script().name());
    }

    @Test
    void processNoMatchStops() {
        Script lobby = scriptWithAliases("lobby", List.of());
        ScriptResolutionStage stage = new ScriptResolutionStage(managerWith(List.of(lobby)));
        ExecutionContext ctx = contextWithName("arena");

        var result = new AtomicReference<ExecutionResult>();
        stage.process(ctx, result::set);

        assertInstanceOf(ExecutionResult.Stop.class, result.get());
    }

    @Test
    void processNullAliasesHandledGracefully() {
        Script noAliases = scriptWithAliases("lobby", null);
        ScriptResolutionStage stage = new ScriptResolutionStage(managerWith(List.of(noAliases)));
        ExecutionContext ctx = contextWithName("unknown");

        var result = new AtomicReference<ExecutionResult>();
        stage.process(ctx, result::set);

        assertInstanceOf(ExecutionResult.Stop.class, result.get());
    }

    private static ScriptManager managerWith(List<Script> scripts) {
        try {
            ScriptManager manager = new ScriptManager(Path.of("/tmp/cb-test"), null);
            Field field = ScriptManager.class.getDeclaredField("enabled");
            field.setAccessible(true);
            field.set(manager, List.copyOf(scripts));
            return manager;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static Script scriptWithAliases(String name, List<String> aliases) {
        return new Script(4, name, true, null, aliases,
                new Permissions(true, false),
                List.of(new IdMapping("test", null)),
                new Defaults(RunAs.CONSOLE, null, null, null, null),
                List.of(),
                List.of(new CmdMapping("say test", null, null, null, null, null)));
    }

    private static ExecutionContext contextWithName(String invokedName) {
        InvokedCommand invoked = new InvokedCommand(invokedName, List.of(), new SenderContext.Console());
        return new ExecutionContext(invoked, null,
                new VelocityTestDoubles.StubCommandSource(Map.of()),
                null, null, Map.of(), null, -1);
    }
}
