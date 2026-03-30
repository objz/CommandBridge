package dev.objz.commandbridge.velocity.dispatch.stage;

import dev.objz.commandbridge.api.channel.command.RunAs;
import dev.objz.commandbridge.net.payloads.cmd.InvokedCommand;
import dev.objz.commandbridge.net.payloads.cmd.SenderContext;
import dev.objz.commandbridge.scripting.model.Defaults;
import dev.objz.commandbridge.scripting.model.Permissions;
import dev.objz.commandbridge.scripting.model.Script;
import dev.objz.commandbridge.scripting.model.enums.ArgType;
import dev.objz.commandbridge.scripting.model.records.mapping.ArgMapping;
import dev.objz.commandbridge.scripting.model.records.mapping.CmdMapping;
import dev.objz.commandbridge.scripting.model.records.mapping.IdMapping;
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
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests for {@link ArgumentMappingStage} — verifies argument mapping from invoked
 * arguments to script-defined argument definitions.
 */
class ArgumentMappingStageTest {

    @BeforeAll
    static void setUp() {
        TestFixtures.ensureLog();
    }

    @Test
    void processAllArgsProvidedContinues() {
        Script script = scriptWithArgs(List.of(
                new ArgMapping("player", true, ArgType.STRING, null),
                new ArgMapping("message", true, ArgType.STRING, null)
        ));
        List<InvokedCommand.TypedArgument> invoked = List.of(
                new InvokedCommand.TypedArgument(ArgType.STRING, "Steve"),
                new InvokedCommand.TypedArgument(ArgType.STRING, "hello")
        );
        ExecutionContext ctx = contextWithArgs(script, invoked);

        var result = new AtomicReference<ExecutionResult>();
        new ArgumentMappingStage().process(ctx, result::set);

        var cont = assertInstanceOf(ExecutionResult.Continue.class, result.get());
        Map<String, Object> args = cont.context().arguments();
        assertEquals("Steve", args.get("player"));
        assertEquals("hello", args.get("message"));
    }

    @Test
    void processMissingOptionalArgContinues() {
        Script script = scriptWithArgs(List.of(
                new ArgMapping("reason", false, ArgType.STRING, null)
        ));
        ExecutionContext ctx = contextWithArgs(script, List.of());

        var result = new AtomicReference<ExecutionResult>();
        new ArgumentMappingStage().process(ctx, result::set);

        var cont = assertInstanceOf(ExecutionResult.Continue.class, result.get());
        assertNull(cont.context().arguments().get("reason"));
    }

    @Test
    void processMissingRequiredArgErrors() {
        Script script = scriptWithArgs(List.of(
                new ArgMapping("target", true, ArgType.STRING, null)
        ));
        ExecutionContext ctx = contextWithArgs(script, List.of());

        var result = new AtomicReference<ExecutionResult>();
        new ArgumentMappingStage().process(ctx, result::set);

        assertInstanceOf(ExecutionResult.Error.class, result.get());
    }

    @Test
    void processExtraArgsIgnored() {
        Script script = scriptWithArgs(List.of(
                new ArgMapping("name", false, ArgType.STRING, null)
        ));
        List<InvokedCommand.TypedArgument> invoked = List.of(
                new InvokedCommand.TypedArgument(ArgType.STRING, "Alice"),
                new InvokedCommand.TypedArgument(ArgType.STRING, "extra1"),
                new InvokedCommand.TypedArgument(ArgType.STRING, "extra2")
        );
        ExecutionContext ctx = contextWithArgs(script, invoked);

        var result = new AtomicReference<ExecutionResult>();
        new ArgumentMappingStage().process(ctx, result::set);

        var cont = assertInstanceOf(ExecutionResult.Continue.class, result.get());
        assertEquals(1, cont.context().arguments().size());
        assertEquals("Alice", cont.context().arguments().get("name"));
    }

    @Test
    void processNoArgsExpectedContinues() {
        Script script = scriptWithArgs(List.of());
        ExecutionContext ctx = contextWithArgs(script, List.of());

        var result = new AtomicReference<ExecutionResult>();
        new ArgumentMappingStage().process(ctx, result::set);

        var cont = assertInstanceOf(ExecutionResult.Continue.class, result.get());
        assertEquals(Map.of(), cont.context().arguments());
    }

    private static Script scriptWithArgs(List<ArgMapping> args) {
        return new Script(4, "test", true, null, List.of(),
                new Permissions(true, false),
                List.of(new IdMapping("test", null)),
                new Defaults(RunAs.CONSOLE, null, null, null, null),
                args,
                List.of(new CmdMapping("say test", null, null, null, null, null)));
    }

    private static ExecutionContext contextWithArgs(Script script, List<InvokedCommand.TypedArgument> invokedArgs) {
        InvokedCommand invoked = new InvokedCommand("test", invokedArgs, new SenderContext.Console());
        return new ExecutionContext(invoked, null,
                new VelocityTestDoubles.StubCommandSource(Map.of()),
                null, script, Map.of(), null, -1);
    }
}
