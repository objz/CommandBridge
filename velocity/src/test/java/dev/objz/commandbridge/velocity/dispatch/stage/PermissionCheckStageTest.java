package dev.objz.commandbridge.velocity.dispatch.stage;

import dev.objz.commandbridge.api.channel.command.RunAs;
import dev.objz.commandbridge.net.payloads.cmd.InvokedCommand;
import dev.objz.commandbridge.net.payloads.cmd.SenderContext;
import dev.objz.commandbridge.scripting.model.Defaults;
import dev.objz.commandbridge.scripting.model.Permissions;
import dev.objz.commandbridge.scripting.model.Script;
import dev.objz.commandbridge.scripting.model.records.mapping.CmdMapping;
import dev.objz.commandbridge.scripting.model.records.mapping.IdMapping;
import dev.objz.commandbridge.velocity.TestFixtures;
import dev.objz.commandbridge.velocity.VelocityTestDoubles;
import dev.objz.commandbridge.velocity.dispatch.model.ExecutionContext;
import dev.objz.commandbridge.velocity.dispatch.model.ExecutionResult;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Tests for {@link PermissionCheckStage} — verifies permission gating, disabled
 * permissions bypass, and silent mode behavior.
 */
final class PermissionCheckStageTest {

    private static final String SCRIPT_NAME = "lobby";
    private static final String PERM_NODE = "commandbridge.command." + SCRIPT_NAME;

    @BeforeAll
    static void setUp() {
        TestFixtures.ensureLog();
    }

    @Test
    void processHasPermissionContinues() {
        Script script = scriptWithPerms(new Permissions(true, false));
        var source = new VelocityTestDoubles.StubCommandSource(Map.of(PERM_NODE, true));
        ExecutionContext ctx = contextWith(script, source);

        var result = new AtomicReference<ExecutionResult>();
        new PermissionCheckStage().process(ctx, result::set);

        assertInstanceOf(ExecutionResult.Continue.class, result.get());
    }

    @Test
    void processNoPermissionStops() {
        Script script = scriptWithPerms(new Permissions(true, false));
        var source = new VelocityTestDoubles.StubCommandSource(Map.of());
        ExecutionContext ctx = contextWith(script, source);

        var result = new AtomicReference<ExecutionResult>();
        new PermissionCheckStage().process(ctx, result::set);

        assertInstanceOf(ExecutionResult.Stop.class, result.get());
    }

    @Test
    void processPermissionsDisabledContinues() {
        Script script = scriptWithPerms(new Permissions(false, false));
        var source = new VelocityTestDoubles.StubCommandSource(Map.of());
        ExecutionContext ctx = contextWith(script, source);

        var result = new AtomicReference<ExecutionResult>();
        new PermissionCheckStage().process(ctx, result::set);

        assertInstanceOf(ExecutionResult.Continue.class, result.get());
    }

    @Test
    void processNoPermissionSilentModeDoesNotSendMessage() {
        Script script = scriptWithPerms(new Permissions(true, true));
        var source = new TrackingCommandSource(Map.of());
        ExecutionContext ctx = contextWith(script, source);

        var result = new AtomicReference<ExecutionResult>();
        new PermissionCheckStage().process(ctx, result::set);

        assertInstanceOf(ExecutionResult.Stop.class, result.get());
        assertFalse(source.messageSent);
    }

    private static Script scriptWithPerms(Permissions permissions) {
        return new Script(4, SCRIPT_NAME, true, null, List.of(),
                permissions,
                List.of(new IdMapping("test", null)),
                new Defaults(RunAs.CONSOLE, null, null, null, null),
                List.of(),
                List.of(new CmdMapping("say test", null, null, null, null, null)));
    }

    private static ExecutionContext contextWith(Script script,
                                                VelocityTestDoubles.StubCommandSource source) {
        InvokedCommand invoked = new InvokedCommand("test", List.of(), new SenderContext.Console());
        return new ExecutionContext(invoked, null, source, null, script, Map.of(), null, -1);
    }

    private static class TrackingCommandSource extends VelocityTestDoubles.StubCommandSource {
        boolean messageSent;

        TrackingCommandSource(Map<String, Boolean> permissions) {
            super(permissions);
        }

        @Override
        public void sendMessage(Component message) {
            messageSent = true;
        }
    }
}
