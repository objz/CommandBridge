package dev.objz.commandbridge.velocity.dispatch;

import dev.objz.commandbridge.api.channel.command.RunAs;
import dev.objz.commandbridge.scripting.model.Defaults;
import dev.objz.commandbridge.scripting.model.Permissions;
import dev.objz.commandbridge.scripting.model.Script;
import dev.objz.commandbridge.scripting.model.records.mapping.CmdMapping;
import dev.objz.commandbridge.scripting.model.records.mapping.IdMapping;
import dev.objz.commandbridge.velocity.TestFixtures;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link CommandDispatcher} — exercises the private {@code buildOPPermissions}
 * method via reflection, verifying permission set construction from script names and
 * command strings.
 */
final class CommandDispatcherTest {

    private static CommandDispatcher dispatcher;
    private static Method buildOPPermissions;

    @BeforeAll
    static void setUp() throws Exception {
        TestFixtures.ensureLog();
        dispatcher = new CommandDispatcher(null, null, null, null);
        buildOPPermissions = CommandDispatcher.class.getDeclaredMethod(
                "buildOPPermissions", Script.class, CmdMapping.class);
        buildOPPermissions.setAccessible(true);
    }

    @Test
    void buildOpPermissionsSingleCommandReturnsBaseAndWildcard() throws Exception {
        Set<String> result = invoke(script("test-script"), cmd("say hello"));

        assertEquals(3, result.size());
        assertTrue(result.contains("commandbridge.command.test-script"));
        assertTrue(result.contains("say"));
        assertTrue(result.contains("say.*"));
    }

    @Test
    void buildOpPermissionsCommandWithSlashStripsSlash() throws Exception {
        Set<String> result = invoke(script("test-script"), cmd("/say hello"));

        assertTrue(result.contains("say"));
        assertTrue(result.contains("say.*"));
        assertFalse(result.contains("/say"));
    }

    @Test
    void buildOpPermissionsSlashOnlyCommandReturnsScriptPermOnly() throws Exception {
        Set<String> result = invoke(script("test-script"), cmd("/"));

        assertEquals(Set.of("commandbridge.command.test-script"), result);
    }

    @Test
    void buildOpPermissionsBlankCommandReturnsScriptPermOnly() throws Exception {
        Set<String> result = invoke(script("test-script"), cmd("   "));

        assertEquals(Set.of("commandbridge.command.test-script"), result);
    }

    @Test
    void buildOpPermissionsNullCommandReturnsScriptPermOnly() throws Exception {
        Set<String> result = invoke(script("test-script"), cmd(null));

        assertEquals(Set.of("commandbridge.command.test-script"), result);
    }

    @Test
    void buildOpPermissionsCommandWithSpacesExtractsFirstWord() throws Exception {
        Set<String> result = invoke(script("test-script"), cmd("teleport player1 player2 100 64 200"));

        assertTrue(result.contains("teleport"));
        assertTrue(result.contains("teleport.*"));
        assertFalse(result.contains("player1"));
    }

    @Test
    void buildOpPermissionsAlwaysIncludesScriptPermission() throws Exception {
        Set<String> result = invoke(script("my-cmd"), cmd("gamemode creative"));

        assertTrue(result.contains("commandbridge.command.my-cmd"));
        assertTrue(result.contains("gamemode"));
        assertTrue(result.contains("gamemode.*"));
    }

    @Test
    void buildOpPermissionsCommandWithLeadingSpacesTrimsCorrectly() throws Exception {
        Set<String> result = invoke(script("test-script"), cmd("  say hello  "));

        assertEquals(3, result.size());
        assertTrue(result.contains("say"));
        assertTrue(result.contains("say.*"));
    }

    @SuppressWarnings("unchecked")
    private static Set<String> invoke(Script script, CmdMapping cmd) throws Exception {
        return (Set<String>) buildOPPermissions.invoke(dispatcher, script, cmd);
    }

    private static Script script(String name) {
        return new Script(4, name, true, null, List.of(),
                new Permissions(true, false),
                List.of(new IdMapping("test", null)),
                new Defaults(RunAs.CONSOLE, null, null, null, null),
                List.of(), List.of());
    }

    private static CmdMapping cmd(String command) {
        return new CmdMapping(command, null, null, null, null, null);
    }
}
