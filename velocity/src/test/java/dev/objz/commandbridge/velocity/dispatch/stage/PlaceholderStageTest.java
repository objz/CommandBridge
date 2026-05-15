package dev.objz.commandbridge.velocity.dispatch.stage;

import dev.objz.commandbridge.net.payloads.cmd.InvokedCommand;
import dev.objz.commandbridge.net.payloads.cmd.SenderContext;
import dev.objz.commandbridge.scripting.model.enums.Location;
import dev.objz.commandbridge.scripting.model.records.mapping.CmdMapping;
import dev.objz.commandbridge.scripting.model.records.mapping.IdMapping;
import dev.objz.commandbridge.scripting.platform.PlatformFeatures;
import dev.objz.commandbridge.velocity.TestFixtures;
import dev.objz.commandbridge.velocity.VelocityTestDoubles;
import dev.objz.commandbridge.velocity.dispatch.model.ExecutionContext;
import dev.objz.commandbridge.velocity.dispatch.model.ExecutionResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Tests for {@link PlaceholderStage}.
 * Verifies ${key} argument substitution in command strings.
 * PlaceholderAPI integration is not tested — it uses a static external factory
 * and requires runtime PAPI installation. Verified at runtime.
 */
final class PlaceholderStageTest {

    private final PlaceholderStage stage = new PlaceholderStage(PlatformFeatures.none());

    @BeforeAll
    static void setUp() {
        TestFixtures.ensureLog();
    }

    @Test
    void processNoPlaceholdersCommandUnchanged() {
        ExecutionContext ctx = contextWithCommand("say hello", Map.of("name", "world"));

        var result = new AtomicReference<ExecutionResult>();
        stage.process(ctx, result::set);

        var cont = assertInstanceOf(ExecutionResult.Continue.class, result.get());
        assertEquals("say hello", cont.context().currentCommand().command());
    }

    @Test
    void processSinglePlaceholderSubstituted() {
        ExecutionContext ctx = contextWithCommand("say ${name}", Map.of("name", "hello"));

        var result = new AtomicReference<ExecutionResult>();
        stage.process(ctx, result::set);

        var cont = assertInstanceOf(ExecutionResult.Continue.class, result.get());
        assertEquals("say hello", cont.context().currentCommand().command());
    }

    @Test
    void processMultiplePlaceholdersAllSubstituted() {
        ExecutionContext ctx = contextWithCommand(
                "tell ${target} ${message}",
                Map.of("target", "Steve", "message", "hello world"));

        var result = new AtomicReference<ExecutionResult>();
        stage.process(ctx, result::set);

        var cont = assertInstanceOf(ExecutionResult.Continue.class, result.get());
        assertEquals("tell Steve hello world", cont.context().currentCommand().command());
    }

    @Test
    void processMissingPlaceholderKeyHandled() {
        ExecutionContext ctx = contextWithCommand("say ${unknown}", Map.of());

        var result = new AtomicReference<ExecutionResult>();
        stage.process(ctx, result::set);

        var cont = assertInstanceOf(ExecutionResult.Continue.class, result.get());
        assertEquals("say ", cont.context().currentCommand().command());
    }

    @Test
    void processCollectionValueJoinedWithSpace() {
        ExecutionContext ctx = contextWithCommand(
                "give ${players} diamond",
                Map.of("players", List.of("Alice", "Bob", "Charlie")));

        var result = new AtomicReference<ExecutionResult>();
        stage.process(ctx, result::set);

        var cont = assertInstanceOf(ExecutionResult.Continue.class, result.get());
        assertEquals("give Alice Bob Charlie diamond", cont.context().currentCommand().command());
    }

    @Test
    void processExecuteIdPlaceholderSubstituted() {
        var input = List.of(new IdMapping("${target}", Location.BACKEND));
        ExecutionContext ctx = contextWithCommandAndExecute(
                "no-placeholder",
                input,
                Map.of("target", "resolved-A"));

        var result = new AtomicReference<ExecutionResult>();
        stage.process(ctx, result::set);

        var cont = assertInstanceOf(ExecutionResult.Continue.class, result.get());
        var execute = cont.context().currentCommand().execute();
        assertEquals(1, execute.size());
        assertEquals("resolved-A", execute.get(0).id());
        assertEquals(Location.BACKEND, execute.get(0).location());
    }

    @Test
    void processExecuteIdAndCommandResolvedTogether() {
        var input = List.of(
                new IdMapping("${target}", Location.BACKEND),
                new IdMapping("literal-id", Location.VELOCITY));
        ExecutionContext ctx = contextWithCommandAndExecute(
                "run ${user}",
                input,
                Map.of("target", "resolved-A", "user", "resolved-B"));

        var result = new AtomicReference<ExecutionResult>();
        stage.process(ctx, result::set);

        var cont = assertInstanceOf(ExecutionResult.Continue.class, result.get());
        var cmd = cont.context().currentCommand();

        assertEquals("run resolved-B", cmd.command());
        assertEquals(2, cmd.execute().size());
        assertEquals("resolved-A", cmd.execute().get(0).id());
        assertEquals(Location.BACKEND, cmd.execute().get(0).location());
        assertEquals("literal-id", cmd.execute().get(1).id());
        assertEquals(Location.VELOCITY, cmd.execute().get(1).location());
    }

    @Test
    void processExecuteIdMixedPlaceholderAndLiteralInSameString() {
        var input = List.of(new IdMapping("prefix-${target}-suffix", Location.BACKEND));
        ExecutionContext ctx = contextWithCommandAndExecute(
                "no-placeholder",
                input,
                Map.of("target", "X"));

        var result = new AtomicReference<ExecutionResult>();
        stage.process(ctx, result::set);

        var cont = assertInstanceOf(ExecutionResult.Continue.class, result.get());
        assertEquals("prefix-X-suffix", cont.context().currentCommand().execute().get(0).id());
    }

    @Test
    void processExecuteIdMissingArgYieldsEmptyId() {
        var input = List.of(new IdMapping("${unknown}", Location.BACKEND));
        ExecutionContext ctx = contextWithCommandAndExecute("no-placeholder", input, Map.of());

        var result = new AtomicReference<ExecutionResult>();
        stage.process(ctx, result::set);

        var cont = assertInstanceOf(ExecutionResult.Continue.class, result.get());
        var execute = cont.context().currentCommand().execute();
        assertEquals(1, execute.size());
        assertEquals("", execute.get(0).id());
    }

    @Test
    void processOnlyExecuteHasPlaceholderCommandUnchanged() {
        String command = "literal command without placeholders";
        var input = List.of(new IdMapping("${target}", Location.BACKEND));
        ExecutionContext ctx = contextWithCommandAndExecute(
                command, input, Map.of("target", "resolved-A"));

        var result = new AtomicReference<ExecutionResult>();
        stage.process(ctx, result::set);

        var cont = assertInstanceOf(ExecutionResult.Continue.class, result.get());
        var cmd = cont.context().currentCommand();
        assertEquals(command, cmd.command());
        assertEquals("resolved-A", cmd.execute().get(0).id());
    }

    @Test
    void processExecuteWithoutPlaceholdersPreservesInputList() {
        var input = List.of(
                new IdMapping("literal-A", Location.BACKEND),
                new IdMapping("literal-B", Location.VELOCITY));
        ExecutionContext ctx = contextWithCommandAndExecute(
                "no-placeholder", input, Map.of("target", "unused"));

        var result = new AtomicReference<ExecutionResult>();
        stage.process(ctx, result::set);

        var cont = assertInstanceOf(ExecutionResult.Continue.class, result.get());
        assertSame(input, cont.context().currentCommand().execute());
    }

    private static ExecutionContext contextWithCommand(String command, Map<String, Object> args) {
        return contextWithCommandAndExecute(command, null, args);
    }

    private static ExecutionContext contextWithCommandAndExecute(
            String command, List<IdMapping> execute, Map<String, Object> args) {
        var source = new VelocityTestDoubles.StubCommandSource(Map.of());
        var script = VelocityTestDoubles.script("test");
        var cmd = new CmdMapping(command, null, execute, null, null, null);
        var invoked = new InvokedCommand("test", List.of(), new SenderContext.Console());
        return new ExecutionContext(invoked, null, source, null, script, args, cmd, 0);
    }
}
