package dev.objz.commandbridge.velocity.setup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VelocityTomlTest {

    @Test
    void aCommentAfterTheServersHeaderDoesNotCreateADuplicateTable() {
        String input = "[servers] # backend addresses\nlobby = \"localhost:25565\"\n";

        String out = VelocityToml.addServers(input, Map.of("creative", "localhost:25566"));

        assertEquals(1, out.split("\\[servers\\]", -1).length - 1);
        assertTrue(out.contains("creative = \"localhost:25566\""));
    }

    // Velocity's own default velocity.toml, comments included
    private static String fixture() throws IOException {
        try (InputStream in = VelocityTomlTest.class.getResourceAsStream("/setup/velocity.toml")) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void insertsBeforeTry() throws IOException {
        String in = fixture();
        String out = VelocityToml.addServers(in, Map.of("creative", "10.0.0.13:25565"));

        String expected = in.replace("try = [", "creative = \"10.0.0.13:25565\"\ntry = [");
        assertEquals(expected, out);
    }

    @Test
    void rejectsTheReservedTryNameWithOrWithoutAnExistingTryArray() throws IOException {
        for (String input : java.util.List.of(fixture(), "[servers]\n\"try\" = [\"lobby\"]\n", "")) {
            assertThrows(IllegalArgumentException.class,
                    () -> VelocityToml.addServers(input, Map.of("try", "localhost:25565")));
        }
    }

    @Test
    void aReservedNameDoesNotChangeTheFile(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("velocity.toml");
        String before = fixture();
        Files.writeString(file, before);

        assertThrows(IllegalArgumentException.class,
                () -> VelocityToml.addServers(file, Map.of("try", "localhost:25565")));
        assertEquals(before, Files.readString(file));
    }

    @Test
    void keepsEveryOtherLine() throws IOException {
        String in = fixture();
        String out = VelocityToml.addServers(in, Map.of("creative", "10.0.0.13:25565"));

        assertEquals(in, out.replace("creative = \"10.0.0.13:25565\"\n", ""));
    }

    @Test
    void skipsExistingNames() throws IOException {
        String in = fixture();
        Map<String, String> servers = new LinkedHashMap<>();
        servers.put("lobby", "10.0.0.99:25565");
        servers.put("creative", "10.0.0.13:25565");
        String out = VelocityToml.addServers(in, servers);

        assertEquals(in.replace("try = [", "creative = \"10.0.0.13:25565\"\ntry = ["), out);
        assertEquals(in, VelocityToml.addServers(in, Map.of("lobby", "10.0.0.99:25565")));
    }

    @Test
    void keepsCrlf() throws IOException {
        String in = fixture().replace("\n", "\r\n");
        String out = VelocityToml.addServers(in, Map.of("creative", "10.0.0.13:25565"));

        assertEquals(in.replace("try = [", "creative = \"10.0.0.13:25565\"\r\ntry = ["), out);
    }

    @Test
    void addsServersTableWhenMissing() {
        String in = "bind = \"0.0.0.0:25565\"\n\n[advanced]\ncompression-level = -1\n";
        String out = VelocityToml.addServers(in, Map.of("creative", "10.0.0.13:25565"));

        assertEquals(in + "\n[servers]\ncreative = \"10.0.0.13:25565\"\n", out);
    }

    @Test
    void appendsAtTableEndWithoutTry() {
        String in = "[servers]\nlobby = \"127.0.0.1:30066\"\n\n[forced-hosts]\n";
        String out = VelocityToml.addServers(in, Map.of("creative", "10.0.0.13:25565"));

        assertEquals("[servers]\nlobby = \"127.0.0.1:30066\"\ncreative = \"10.0.0.13:25565\"\n\n[forced-hosts]\n", out);
    }

    @Test
    void writesFileInPlace(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("velocity.toml");
        Files.writeString(file, fixture());
        VelocityToml.addServers(file, Map.of("creative", "10.0.0.13:25565"));

        assertTrue(Files.readString(file).contains("creative = \"10.0.0.13:25565\"\ntry = ["));
        assertTrue(Files.list(dir).allMatch(p -> p.equals(file)));
    }
}
