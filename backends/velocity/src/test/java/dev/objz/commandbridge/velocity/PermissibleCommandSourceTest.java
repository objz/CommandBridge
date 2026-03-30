package dev.objz.commandbridge.velocity;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.permission.Tristate;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the {@code PermissibleCommandSource} inner class of {@link VelocityExecutor}.
 * Verifies that granted permissions override the delegate and that non-granted permissions
 * are correctly delegated to the wrapped {@link CommandSource}.
 */
final class PermissibleCommandSourceTest {

    @BeforeAll
    static void setUp() {
        TestFixtures.ensureLog();
    }

    /** Minimal stub for testing permission delegation. */
    static CommandSource stub(Map<String, Boolean> permissions) {
        return new CommandSource() {
            @Override
            public boolean hasPermission(String permission) {
                return permissions.getOrDefault(permission, false);
            }

            @Override
            public Tristate getPermissionValue(String permission) {
                Boolean v = permissions.get(permission);
                if (v == null) {
                    return Tristate.UNDEFINED;
                }
                return v ? Tristate.TRUE : Tristate.FALSE;
            }

            @Override
            public void sendMessage(Component message) {
            }
        };
    }

    private static CommandSource createPermissible(CommandSource delegate, Set<String> granted)
            throws ReflectiveOperationException {
        Class<?> cls = Class.forName(
                "dev.objz.commandbridge.velocity.VelocityExecutor$PermissibleCommandSource");
        Constructor<?> ctor = cls.getDeclaredConstructor(CommandSource.class, Set.class);
        ctor.setAccessible(true);
        return (CommandSource) ctor.newInstance(delegate, granted);
    }

    @Test
    void hasPermissionGrantedPermissionReturnsTrue() throws ReflectiveOperationException {
        CommandSource delegate = stub(Map.of());
        CommandSource pcs = createPermissible(delegate, Set.of("my.granted"));

        assertTrue(pcs.hasPermission("my.granted"));
    }

    @Test
    void hasPermissionNotGrantedDelegatesToOriginal() throws ReflectiveOperationException {
        CommandSource delegate = stub(Map.of("delegate.perm", true));
        CommandSource pcs = createPermissible(delegate, Set.of("other.perm"));

        assertTrue(pcs.hasPermission("delegate.perm"));
        assertFalse(pcs.hasPermission("unknown.perm"));
    }

    @Test
    void getPermissionValueGrantedPermissionReturnsTrue() throws ReflectiveOperationException {
        CommandSource delegate = stub(Map.of());
        CommandSource pcs = createPermissible(delegate, Set.of("granted.node"));

        assertEquals(Tristate.TRUE, pcs.getPermissionValue("granted.node"));
    }

    @Test
    void getPermissionValueNotGrantedDelegatesToOriginal() throws ReflectiveOperationException {
        CommandSource delegate = stub(Map.of(
                "yes.perm", true,
                "no.perm", false
        ));
        CommandSource pcs = createPermissible(delegate, Set.of("other"));

        assertEquals(Tristate.TRUE, pcs.getPermissionValue("yes.perm"));
        assertEquals(Tristate.FALSE, pcs.getPermissionValue("no.perm"));
        assertEquals(Tristate.UNDEFINED, pcs.getPermissionValue("missing.perm"));
    }

    @Test
    void grantedPermissionOverridesOriginalFalse() throws ReflectiveOperationException {
        CommandSource delegate = stub(Map.of("overridden.perm", false));
        CommandSource pcs = createPermissible(delegate, Set.of("overridden.perm"));

        assertTrue(pcs.hasPermission("overridden.perm"));
        assertEquals(Tristate.TRUE, pcs.getPermissionValue("overridden.perm"));
    }
}
