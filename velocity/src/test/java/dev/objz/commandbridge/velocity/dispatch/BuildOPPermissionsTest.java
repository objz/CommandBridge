package dev.objz.commandbridge.velocity.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import dev.objz.commandbridge.api.channel.command.RunAs;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.scripting.model.Defaults;
import dev.objz.commandbridge.scripting.model.Permissions;
import dev.objz.commandbridge.scripting.model.Script;
import dev.objz.commandbridge.scripting.model.records.mapping.CmdMapping;

class BuildOPPermissionsTest {

    @BeforeAll
    static void installLog() {
        try {
            Log.install(java.util.logging.Logger.getLogger("test"));
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    void normalCommand() throws Exception {
        CommandDispatcher dispatcher = new CommandDispatcher(null, null, null, null);
        Script script = new Script(4, "test-script", true, null, null,
                new Permissions(true, false), List.of(), 
                new Defaults(RunAs.CONSOLE, null, null, Duration.ZERO, Duration.ZERO),
                List.of(), List.of());
        CmdMapping mapping = new CmdMapping("gamemode creative", null, null, null, null, null);

        Set<String> result = invokeMethod(dispatcher, script, mapping);

        assertEquals(3, result.size());
        assertTrue(result.contains("commandbridge.command.test-script"));
        assertTrue(result.contains("gamemode"));
        assertTrue(result.contains("gamemode.*"));
    }

    @Test
    void commandWithSlash() throws Exception {
        CommandDispatcher dispatcher = new CommandDispatcher(null, null, null, null);
        Script script = new Script(4, "test-script", true, null, null,
                new Permissions(true, false), List.of(), 
                new Defaults(RunAs.CONSOLE, null, null, Duration.ZERO, Duration.ZERO),
                List.of(), List.of());
        CmdMapping mapping = new CmdMapping("/gamemode creative", null, null, null, null, null);

        Set<String> result = invokeMethod(dispatcher, script, mapping);

        assertEquals(3, result.size());
        assertTrue(result.contains("commandbridge.command.test-script"));
        assertTrue(result.contains("gamemode"));
        assertTrue(result.contains("gamemode.*"));
    }

    @Test
    void blankCommand() throws Exception {
        CommandDispatcher dispatcher = new CommandDispatcher(null, null, null, null);
        Script script = new Script(4, "test-script", true, null, null,
                new Permissions(true, false), List.of(), 
                new Defaults(RunAs.CONSOLE, null, null, Duration.ZERO, Duration.ZERO),
                List.of(), List.of());
        CmdMapping mapping = new CmdMapping("", null, null, null, null, null);

        Set<String> result = invokeMethod(dispatcher, script, mapping);

        assertEquals(1, result.size());
        assertTrue(result.contains("commandbridge.command.test-script"));
    }

    @Test
    void nullCommand() throws Exception {
        CommandDispatcher dispatcher = new CommandDispatcher(null, null, null, null);
        Script script = new Script(4, "test-script", true, null, null,
                new Permissions(true, false), List.of(), 
                new Defaults(RunAs.CONSOLE, null, null, Duration.ZERO, Duration.ZERO),
                List.of(), List.of());
        CmdMapping mapping = new CmdMapping(null, null, null, null, null, null);

        Set<String> result = invokeMethod(dispatcher, script, mapping);

        assertEquals(1, result.size());
        assertTrue(result.contains("commandbridge.command.test-script"));
    }

    @Test
    void whitespaceOnly() throws Exception {
        CommandDispatcher dispatcher = new CommandDispatcher(null, null, null, null);
        Script script = new Script(4, "test-script", true, null, null,
                new Permissions(true, false), List.of(), 
                new Defaults(RunAs.CONSOLE, null, null, Duration.ZERO, Duration.ZERO),
                List.of(), List.of());
        CmdMapping mapping = new CmdMapping("   ", null, null, null, null, null);

        Set<String> result = invokeMethod(dispatcher, script, mapping);

        assertEquals(1, result.size());
        assertTrue(result.contains("commandbridge.command.test-script"));
    }

    @Test
    void multipleSpaces() throws Exception {
        CommandDispatcher dispatcher = new CommandDispatcher(null, null, null, null);
        Script script = new Script(4, "test-script", true, null, null,
                new Permissions(true, false), List.of(), 
                new Defaults(RunAs.CONSOLE, null, null, Duration.ZERO, Duration.ZERO),
                List.of(), List.of());
        CmdMapping mapping = new CmdMapping("gamemode  creative", null, null, null, null, null);

        Set<String> result = invokeMethod(dispatcher, script, mapping);

        assertEquals(3, result.size());
        assertTrue(result.contains("commandbridge.command.test-script"));
        assertTrue(result.contains("gamemode"));
        assertTrue(result.contains("gamemode.*"));
    }

    @SuppressWarnings("unchecked")
    private static Set<String> invokeMethod(CommandDispatcher dispatcher, Script script,
            CmdMapping mapping) throws Exception {
        Method method = CommandDispatcher.class.getDeclaredMethod("buildOPPermissions",
                Script.class, CmdMapping.class);
        method.setAccessible(true);
        return (Set<String>) method.invoke(dispatcher, script, mapping);
    }
}
