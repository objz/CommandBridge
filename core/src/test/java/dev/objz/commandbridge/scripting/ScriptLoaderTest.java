package dev.objz.commandbridge.scripting;

import dev.objz.commandbridge.TestFixtures;
import dev.objz.commandbridge.api.channel.command.RunAs;
import dev.objz.commandbridge.scripting.model.Script;
import dev.objz.commandbridge.scripting.model.enums.ArgType;
import dev.objz.commandbridge.scripting.model.enums.Location;
import dev.objz.commandbridge.scripting.model.records.mapping.CmdMapping;
import dev.objz.commandbridge.scripting.platform.PlatformFeatureKeys;
import dev.objz.commandbridge.scripting.platform.PlatformFeatureSet;
import dev.objz.commandbridge.scripting.platform.PlatformFeatures;
import dev.objz.commandbridge.scripting.validation.ProblemSink;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ScriptLoaderTest {

    private static final PlatformFeatures PACKET_EVENTS_ON_VELOCITY = PlatformFeatureSet.builder()
            .add(Location.VELOCITY, PlatformFeatureKeys.PACKET_EVENTS)
            .build();

    @BeforeAll
    static void setUp() {
        TestFixtures.ensureLog();
    }

    private static final String VALID = """
            version: 4
            name: give-item
            permissions:
              enabled: true
              silent: false
            register:
              - id: proxy-1
                location: VELOCITY
            defaults:
              run-as: CONSOLE
              execute:
                - id: survival-1
                  location: BACKEND
              server:
                target-required: true
                player-arg: player
              delay: 2s
            args:
              - name: player
                required: true
                type: STRING
              - name: amount
                type: INTEGER
            commands:
              - command: "give ${player} diamond ${amount}"
            """;

    private static ScriptLoader.LoadResult<Script> load(String yaml) {
        return load(yaml, PlatformFeatures.none());
    }

    private static ScriptLoader.LoadResult<Script> load(String yaml, PlatformFeatures features) {
        return ScriptLoader.loadResult(Script.class,
                new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)), features);
    }

    private static ScriptLoader.LoadResult<Script> loadResource(String name, PlatformFeatures features)
            throws IOException {
        try (InputStream in = ScriptLoaderTest.class.getResourceAsStream("/scripts/" + name)) {
            assertNotNull(in, "missing test resource " + name);
            return ScriptLoader.loadResult(Script.class, in, features);
        }
    }

    private static boolean hasProblem(ProblemSink sink, String pathPart, String messagePart) {
        return sink.problems().stream().anyMatch(p -> (pathPart == null
                || (p.path() != null && p.path().contains(pathPart)))
                && p.message().contains(messagePart));
    }

    @Test
    void validScriptLoadsWithoutProblems() {
        var result = load(VALID);

        assertTrue(result.ok(), result.problems.toString());
        Script script = result.value;
        assertEquals("give-item", script.name());
        assertEquals(2, script.args().size());
        assertEquals(ArgType.INTEGER, script.args().get(1).type());
        assertEquals(1, script.commands().size());
    }

    @Test
    void shippedExampleLoadsWithoutProblemsWhenPacketEventsIsAvailable() throws IOException {
        var result = loadResource("example.yml", PACKET_EVENTS_ON_VELOCITY);

        assertTrue(result.ok(), result.problems.toString());
        assertEquals("example", result.value.name());
        assertFalse(result.value.enabled());
    }

    @Test
    void shippedExampleLoadsWithoutProblemsOnPlainVelocity() throws IOException {
        var result = loadResource("example.yml", PlatformFeatures.none());

        assertTrue(result.ok(), result.problems.toString());
    }

    @Test
    void oldVersionIsRejectedByMin() {
        var result = load(VALID.replace("version: 4", "version: 3"));

        assertFalse(result.ok());
        assertTrue(hasProblem(result.problems, "version", "below minimum"), result.problems.toString());
    }

    @Test
    void nameViolatingPatternIsRejected() {
        var result = load(VALID.replace("name: give-item", "name: Ab"));

        assertFalse(result.ok());
        assertTrue(hasProblem(result.problems, "name", "does not match required pattern"),
                result.problems.toString());
    }

    @Test
    void missingCommandsIsRequired() {
        String yaml = VALID.substring(0, VALID.indexOf("commands:"));
        var result = load(yaml);

        assertFalse(result.ok());
        assertTrue(hasProblem(result.problems, "commands", "is required"), result.problems.toString());
        assertEquals(1, result.problems.count(), result.problems.toString());
    }

    @Test
    void enabledDefaultsToTrueWhenOmitted() {
        var result = load(VALID);

        assertTrue(result.ok(), result.problems.toString());
        assertTrue(result.value.enabled());
        assertEquals(RunAs.CONSOLE, result.value.defaults().runAs());
        assertEquals(Duration.ZERO, result.value.defaults().cooldown());
    }

    @Test
    void commandsInheritDefaults() {
        var result = load(VALID);

        assertTrue(result.ok(), result.problems.toString());
        CmdMapping cmd = result.value.commands().getFirst();
        assertEquals(result.value.defaults().server(), cmd.server());
        assertEquals("player", cmd.server().playerArg());
        assertTrue(cmd.server().targetRequired());
        assertEquals(RunAs.CONSOLE, cmd.runAs());
        assertEquals(Duration.ofSeconds(2), cmd.delay());
        assertEquals("survival-1", cmd.execute().getFirst().id());
    }

    @Test
    void commandOverridesWinOverDefaults() {
        var result = load(VALID.replace("""
                  - command: "give ${player} diamond ${amount}"
                """, """
                  - command: "give ${player} diamond ${amount}"
                    run-as: PLAYER
                    delay: 0s
                """));

        assertTrue(result.ok(), result.problems.toString());
        CmdMapping cmd = result.value.commands().getFirst();
        assertEquals(RunAs.PLAYER, cmd.runAs());
        assertEquals(Duration.ZERO, cmd.delay());
        assertEquals("player", cmd.server().playerArg());
    }

    @Test
    void unknownPlayerArgIsRejected() {
        var result = load(VALID.replace("player-arg: player", "player-arg: target"));

        assertFalse(result.ok());
        assertTrue(hasProblem(result.problems, "defaults.server", "unknown argument 'target'"),
                result.problems.toString());
    }

    @Test
    void integerPlayerArgIsRejected() {
        var result = load(VALID.replace("player-arg: player", "player-arg: amount"));

        assertFalse(result.ok());
        assertTrue(hasProblem(result.problems, "defaults.server", "is not player-resolvable"),
                result.problems.toString());
    }

    @Test
    void greedyStringThatIsNotLastIsRejected() {
        var result = load(VALID.replace("type: STRING", "type: GREEDY_STRING"));

        assertFalse(result.ok());
        assertTrue(hasProblem(result.problems, "commands[0]", "must be the last argument"),
                result.problems.toString());
    }

    @Test
    void greedyStringAsLastArgumentIsAccepted() {
        var result = load(VALID.replace("type: INTEGER", "type: GREEDY_STRING"));

        assertTrue(result.ok(), result.problems.toString());
    }

    @Test
    void malformedYamlReportsLineAndColumn() {
        var result = load("""
                version: 4
                name: [broken
                enabled: true
                """);

        assertFalse(result.ok());
        assertNull(result.value);
        assertTrue(result.problems.problems().getFirst().message().matches("YAML error at line \\d+, column \\d+: .*"),
                result.problems.toString());
    }

    @Test
    void argsAsMappingIsReportedAsProblem() {
        String yaml = VALID.substring(0, VALID.indexOf("args:")) + """
                args:
                  player:
                    type: STRING
                commands:
                  - command: "say hi"
                """;

        var result = load(yaml);

        assertFalse(result.ok());
        assertTrue(hasProblem(result.problems, "args", "expected a list"), result.problems.toString());
    }

    @Test
    void invalidListElementIsReportedAsProblem() {
        var result = load(VALID.replace("name: give-item", "name: give-item\naliases: [gi, {x: y}]"));

        assertFalse(result.ok());
        assertTrue(hasProblem(result.problems, "aliases[1]", ""), result.problems.toString());
    }

    @Test
    void backendOnlyArgumentOnVelocityRegistrationIsRejected() {
        var result = load(VALID.replace("type: INTEGER", "type: LOCATION"));

        assertFalse(result.ok());
        assertTrue(hasProblem(result.problems, "register", "only supported on [BACKEND]"),
                result.problems.toString());
    }

    @Test
    void optionalPlatformArgumentIsAcceptedWhenFeatureIsAvailable() {
        String yaml = VALID.replace("type: STRING", "type: PLAYERS");

        assertFalse(load(yaml).ok());
        var result = load(yaml, PACKET_EVENTS_ON_VELOCITY);
        assertTrue(result.ok(), result.problems.toString());
    }

    @Test
    void unknownPlaceholderIsRejected() {
        var result = load(VALID.replace("diamond ${amount}", "diamond ${count}"));

        assertFalse(result.ok());
        assertTrue(hasProblem(result.problems, "command[0]", "Unknown argument 'count'"),
                result.problems.toString());
    }

    @Test
    void emptyPlaceholderIsRejected() {
        var result = load(VALID.replace("diamond ${amount}", "diamond ${}"));

        assertFalse(result.ok());
        assertTrue(hasProblem(result.problems, null, "Empty placeholder"), result.problems.toString());
    }
}
