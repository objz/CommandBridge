package dev.objz.commandbridge.scripting.validation.processor;

import dev.objz.commandbridge.api.channel.command.RunAs;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.scripting.bind.BindContext;
import dev.objz.commandbridge.scripting.bind.RecordBinder;
import dev.objz.commandbridge.scripting.bind.TypeAdapterRegistry;
import dev.objz.commandbridge.scripting.bind.adapters.DurationAdapter;
import dev.objz.commandbridge.scripting.bind.adapters.EnumAdapter;
import dev.objz.commandbridge.scripting.bind.adapters.PrimitivesAdapter;
import dev.objz.commandbridge.scripting.bind.adapters.StringAdapter;
import dev.objz.commandbridge.scripting.model.Defaults;
import dev.objz.commandbridge.scripting.model.Permissions;
import dev.objz.commandbridge.scripting.model.Script;
import dev.objz.commandbridge.scripting.model.enums.ArgType;
import dev.objz.commandbridge.scripting.model.enums.Location;
import dev.objz.commandbridge.scripting.model.records.Server;
import dev.objz.commandbridge.scripting.model.records.mapping.ArgMapping;
import dev.objz.commandbridge.scripting.model.records.mapping.CmdMapping;
import dev.objz.commandbridge.scripting.model.records.mapping.IdMapping;
import dev.objz.commandbridge.scripting.platform.PlatformFeatures;
import dev.objz.commandbridge.scripting.validation.PostProcessor;
import dev.objz.commandbridge.scripting.validation.ProblemSink;
import dev.objz.commandbridge.scripting.validation.ScriptFixtures;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidationProcessorsTest {

    @BeforeAll
    static void installLog() {
        try {
            Log.install(LoggerFactory.getLogger("test"));
        } catch (IllegalStateException e) {
            Log.debug("Log already installed for test context");
        }
    }

    private static BindContext ctx() {
        return new BindContext(adapters(), new ProblemSink(), PlatformFeatures.none());
    }

    private static TypeAdapterRegistry adapters() {
        return new TypeAdapterRegistry()
                .register(new StringAdapter())
                .register(new PrimitivesAdapter())
                .register(new EnumAdapter())
                .register(new DurationAdapter());
    }

    private static RecordBinder.MutableRecordBuffer scriptBuffer(Map<String, Object> overrides) {
        Map<String, Object> values = new HashMap<>();
        values.put("version", 4);
        values.put("name", "valid-script");
        values.put("enabled", true);
        values.put("description", "desc");
        values.put("aliases", List.of("alias"));
        values.put("permissions", new Permissions(true, false));
        values.put("register", List.of(new IdMapping("proxy", Location.VELOCITY)));
        values.put("defaults", new Defaults(
                RunAs.CONSOLE,
                List.of(new IdMapping("default-backend", Location.BACKEND)),
                new Server(false, false, null),
                Duration.ZERO,
                Duration.ZERO));
        values.put("args", List.of(new ArgMapping("target", true, ArgType.STRING, null)));
        values.put("commands", List.of(new CmdMapping("say ${target}", null, null, null, null, null)));
        values.putAll(overrides);
        return ScriptFixtures.createBuffer(Script.class, values);
    }

    private static int indexOf(RecordBinder.MutableRecordBuffer buffer, String fieldName) {
        for (int i = 0; i < buffer.components().length; i++) {
            if (fieldName.equals(buffer.components()[i].getName())) {
                return i;
            }
        }
        throw new IllegalStateException("Missing field: " + fieldName);
    }

    private static List<ProblemSink.Problem> processProblems(PostProcessor processor,
            RecordBinder.MutableRecordBuffer buffer, BindContext context) {
        processor.process(buffer, context);
        return context.problems().problems();
    }

    private static void assertNoErrors(List<ProblemSink.Problem> problems) {
        assertTrue(problems.isEmpty(), "Expected no validation errors, got: " + problems);
    }

    @Nested
    class RequiredProcessorTests {

        @Test
        void nullRequiredFieldAddsError() {
            RecordBinder.MutableRecordBuffer buffer = scriptBuffer(withNull("permissions"));
            BindContext context = ctx();

            List<ProblemSink.Problem> problems = processProblems(new RequiredProcessor(), buffer, context);

            assertEquals(1, problems.size());
            assertEquals("permissions", problems.get(0).path());
        }

        @Test
        void presentRequiredFieldAddsNoError() {
            RecordBinder.MutableRecordBuffer buffer = scriptBuffer(Map.of());
            BindContext context = ctx();

            List<ProblemSink.Problem> problems = processProblems(new RequiredProcessor(), buffer, context);

            assertNoErrors(problems);
        }
    }

    @Nested
    class MinProcessorTests {

        @Test
        void belowMinAddsError() {
            RecordBinder.MutableRecordBuffer buffer = scriptBuffer(Map.of("version", 3));
            BindContext context = ctx();

            List<ProblemSink.Problem> problems = processProblems(new MinProcessor(), buffer, context);

            assertFalse(problems.isEmpty());
            assertTrue(problems.stream().anyMatch(p -> p.path().equals("version")));
        }

        @Test
        void atMinAddsNoError() {
            RecordBinder.MutableRecordBuffer buffer = scriptBuffer(Map.of("version", 4));
            BindContext context = ctx();

            List<ProblemSink.Problem> problems = processProblems(new MinProcessor(), buffer, context);

            assertNoErrors(problems);
        }
    }

    @Nested
    class MaxProcessorTests {

        @Test
        void aboveMaxAddsError() {
            RecordBinder.MutableRecordBuffer buffer = scriptBuffer(Map.of("version", 5));
            BindContext context = ctx();

            List<ProblemSink.Problem> problems = processProblems(new MaxProcessor(), buffer, context);

            assertFalse(problems.isEmpty());
            assertTrue(problems.stream().anyMatch(p -> p.path().equals("version")));
        }

        @Test
        void belowOrAtMaxAddsNoError() {
            RecordBinder.MutableRecordBuffer buffer = scriptBuffer(Map.of("version", 4));
            BindContext context = ctx();

            List<ProblemSink.Problem> problems = processProblems(new MaxProcessor(), buffer, context);

            assertNoErrors(problems);
        }
    }

    @Nested
    class DefaultProcessorTests {

        @Test
        void nullFieldGetsDefaultApplied() {
            RecordBinder.MutableRecordBuffer buffer = scriptBuffer(withNull("enabled"));
            BindContext context = ctx();

            List<ProblemSink.Problem> problems = processProblems(new DefaultProcessor(), buffer, context);

            int enabledIndex = indexOf(buffer, "enabled");
            assertEquals(true, buffer.get(enabledIndex));
            assertNoErrors(problems);
        }

        @Test
        void existingFieldValueIsUnchanged() {
            RecordBinder.MutableRecordBuffer buffer = scriptBuffer(Map.of("enabled", false));
            BindContext context = ctx();

            List<ProblemSink.Problem> problems = processProblems(new DefaultProcessor(), buffer, context);

            int enabledIndex = indexOf(buffer, "enabled");
            assertEquals(false, buffer.get(enabledIndex));
            assertNoErrors(problems);
        }
    }

    @Nested
    class PatternProcessorTests {

        @Test
        void matchingPatternAddsNoError() {
            RecordBinder.MutableRecordBuffer buffer = scriptBuffer(Map.of("name", "good-name"));
            BindContext context = ctx();

            List<ProblemSink.Problem> problems = processProblems(new PatternProcessor(), buffer, context);

            assertNoErrors(problems);
        }

        @Test
        void nonMatchingPatternAddsError() {
            RecordBinder.MutableRecordBuffer buffer = scriptBuffer(Map.of("name", "INVALID_NAME"));
            BindContext context = ctx();

            List<ProblemSink.Problem> problems = processProblems(new PatternProcessor(), buffer, context);

            assertEquals(1, problems.size());
            assertEquals("name", problems.get(0).path());
        }
    }

    @Nested
    class MergeProcessorTests {

        @Test
        void mergeFieldsAreFilledFromDefaults() {
            Defaults defaults = new Defaults(
                    RunAs.PLAYER,
                    List.of(new IdMapping("merged", Location.BACKEND)),
                    new Server(true, true, "target"),
                    Duration.ofSeconds(1),
                    Duration.ofSeconds(2));
            CmdMapping command = new CmdMapping("say ${target}", null, null, null, null, null);

            RecordBinder.MutableRecordBuffer buffer = scriptBuffer(Map.of(
                    "defaults", defaults,
                    "commands", List.of(command)));
            BindContext context = ctx();

            List<ProblemSink.Problem> problems = processProblems(new MergeProcessor(), buffer, context);

            int commandsIndex = indexOf(buffer, "commands");
            List<?> mergedCommands = (List<?>) buffer.get(commandsIndex);
            CmdMapping merged = (CmdMapping) mergedCommands.get(0);

            assertEquals(RunAs.PLAYER, merged.runAs());
            assertEquals(defaults.execute(), merged.execute());
            assertEquals(defaults.server(), merged.server());
            assertEquals(defaults.delay(), merged.delay());
            assertEquals(defaults.cooldown(), merged.cooldown());
            assertNoErrors(problems);
        }

        @Test
        void alreadySetMergeFieldsStayUnchanged() {
            Defaults defaults = new Defaults(
                    RunAs.CONSOLE,
                    List.of(new IdMapping("default", Location.BACKEND)),
                    new Server(false, false, "target"),
                    Duration.ofSeconds(3),
                    Duration.ofSeconds(4));
            CmdMapping command = new CmdMapping(
                    "say ${target}",
                    RunAs.OPERATOR,
                    List.of(new IdMapping("custom", Location.VELOCITY)),
                    new Server(true, false, "target"),
                    Duration.ofSeconds(7),
                    Duration.ofSeconds(8));

            RecordBinder.MutableRecordBuffer buffer = scriptBuffer(Map.of(
                    "defaults", defaults,
                    "commands", List.of(command)));
            BindContext context = ctx();

            List<ProblemSink.Problem> problems = processProblems(new MergeProcessor(), buffer, context);

            int commandsIndex = indexOf(buffer, "commands");
            List<?> mergedCommands = (List<?>) buffer.get(commandsIndex);
            CmdMapping merged = (CmdMapping) mergedCommands.get(0);

            assertEquals(command.runAs(), merged.runAs());
            assertEquals(command.execute(), merged.execute());
            assertEquals(command.server(), merged.server());
            assertEquals(command.delay(), merged.delay());
            assertEquals(command.cooldown(), merged.cooldown());
            assertNoErrors(problems);
        }
    }

    @Nested
    class ArgumentOrderProcessorTests {

        @Test
        void requiredArgumentAfterOptionalAddsError() {
            List<ArgMapping> args = List.of(
                    new ArgMapping("opt", false, ArgType.STRING, null),
                    new ArgMapping("req", true, ArgType.STRING, null));
            List<CmdMapping> commands = List.of(new CmdMapping("run ${opt} ${req}", null, null, null, null, null));
            RecordBinder.MutableRecordBuffer buffer = scriptBuffer(Map.of("args", args, "commands", commands));
            BindContext context = ctx();

            List<ProblemSink.Problem> problems = processProblems(new ArgumentOrderProcessor(), buffer, context);

            assertEquals(1, problems.size());
            assertTrue(problems.get(0).message().contains("Required argument 'req'"));
        }

        @Test
        void correctOrderAddsNoError() {
            List<ArgMapping> args = List.of(
                    new ArgMapping("req", true, ArgType.STRING, null),
                    new ArgMapping("opt", false, ArgType.STRING, null));
            List<CmdMapping> commands = List.of(new CmdMapping("run ${req} ${opt}", null, null, null, null, null));
            RecordBinder.MutableRecordBuffer buffer = scriptBuffer(Map.of("args", args, "commands", commands));
            BindContext context = ctx();

            List<ProblemSink.Problem> problems = processProblems(new ArgumentOrderProcessor(), buffer, context);

            assertNoErrors(problems);
        }
    }

    @Nested
    class TrailingArgumentProcessorTests {

        @Test
        void greedyStringNotLastAddsError() {
            List<ArgMapping> args = List.of(
                    new ArgMapping("tail", true, ArgType.GREEDY_STRING, null),
                    new ArgMapping("next", true, ArgType.STRING, null));
            List<CmdMapping> commands = List.of(new CmdMapping("run ${tail} ${next}", null, null, null, null, null));
            RecordBinder.MutableRecordBuffer buffer = scriptBuffer(Map.of("args", args, "commands", commands));
            BindContext context = ctx();

            List<ProblemSink.Problem> problems = processProblems(new TrailingArgumentProcessor(), buffer, context);

            assertEquals(1, problems.size());
            assertTrue(problems.get(0).message().contains("must be the last argument"));
        }

        @Test
        void greedyStringLastAddsNoError() {
            List<ArgMapping> args = List.of(
                    new ArgMapping("name", true, ArgType.STRING, null),
                    new ArgMapping("tail", true, ArgType.GREEDY_STRING, null));
            List<CmdMapping> commands = List.of(new CmdMapping("run ${name} ${tail}", null, null, null, null, null));
            RecordBinder.MutableRecordBuffer buffer = scriptBuffer(Map.of("args", args, "commands", commands));
            BindContext context = ctx();

            List<ProblemSink.Problem> problems = processProblems(new TrailingArgumentProcessor(), buffer, context);

            assertNoErrors(problems);
        }
    }

    @Nested
    class ResolvableProcessorTests {

        @Test
        void validPlaceholderReferencesAddNoError() {
            List<ArgMapping> args = List.of(new ArgMapping("target", true, ArgType.STRING, null));
            List<CmdMapping> commands = List.of(new CmdMapping("say ${target}", null, null, null, null, null));
            RecordBinder.MutableRecordBuffer buffer = scriptBuffer(Map.of("args", args, "commands", commands));
            BindContext context = ctx();

            List<ProblemSink.Problem> problems = processProblems(new ResolvableProcessor(), buffer, context);

            assertNoErrors(problems);
        }

        @Test
        void unknownPlaceholderReferenceAddsError() {
            List<ArgMapping> args = List.of(new ArgMapping("target", true, ArgType.STRING, null));
            List<CmdMapping> commands = List.of(new CmdMapping("say ${missing}", null, null, null, null, null));
            RecordBinder.MutableRecordBuffer buffer = scriptBuffer(Map.of("args", args, "commands", commands));
            BindContext context = ctx();

            List<ProblemSink.Problem> problems = processProblems(new ResolvableProcessor(), buffer, context);

            assertEquals(1, problems.size());
            assertTrue(problems.get(0).message().contains("Unknown argument 'missing'"));
        }
    }

    @Nested
    class PlatformProcessorTests {

        @Test
        void unsupportedPlatformArgumentCombinationAddsError() {
            List<ArgMapping> args = List.of(new ArgMapping("srv", true, ArgType.SERVER, null));
            List<CmdMapping> commands = List.of(new CmdMapping("send ${srv}", null, null, null, null, null));
            List<IdMapping> register = List.of(new IdMapping("backend-1", Location.BACKEND));
            RecordBinder.MutableRecordBuffer buffer = scriptBuffer(Map.of(
                    "args", args,
                    "commands", commands,
                    "register", register));
            BindContext context = ctx();

            List<ProblemSink.Problem> problems = processProblems(new PlatformProcessor(), buffer, context);

            assertEquals(1, problems.size());
            assertTrue(problems.get(0).message().contains("only supported on [VELOCITY]"));
        }

        @Test
        void supportedPlatformArgumentCombinationAddsNoError() {
            List<ArgMapping> args = List.of(new ArgMapping("target", true, ArgType.STRING, null));
            List<CmdMapping> commands = List.of(new CmdMapping("send ${target}", null, null, null, null, null));
            List<IdMapping> register = List.of(
                    new IdMapping("proxy", Location.VELOCITY),
                    new IdMapping("backend-1", Location.BACKEND));
            RecordBinder.MutableRecordBuffer buffer = scriptBuffer(Map.of(
                    "args", args,
                    "commands", commands,
                    "register", register));
            BindContext context = ctx();

            List<ProblemSink.Problem> problems = processProblems(new PlatformProcessor(), buffer, context);

            assertNoErrors(problems);
        }
    }

    @Nested
    class PlayerArgProcessorTests {

        @Test
        void validPlayerArgReferenceAddsNoError() {
            List<ArgMapping> args = List.of(new ArgMapping("target", true, ArgType.STRING, null));
            Defaults defaults = new Defaults(
                    RunAs.CONSOLE,
                    null,
                    new Server(false, false, "target"),
                    Duration.ZERO,
                    Duration.ZERO);
            RecordBinder.MutableRecordBuffer buffer = scriptBuffer(Map.of("args", args, "defaults", defaults));
            BindContext context = ctx();

            List<ProblemSink.Problem> problems = processProblems(new PlayerArgProcessor(), buffer, context);

            assertNoErrors(problems);
        }

        @Test
        void nonResolvablePlayerArgTypeAddsError() {
            List<ArgMapping> args = List.of(new ArgMapping("target", true, ArgType.INTEGER, null));
            Defaults defaults = new Defaults(
                    RunAs.CONSOLE,
                    null,
                    new Server(false, false, "target"),
                    Duration.ZERO,
                    Duration.ZERO);
            RecordBinder.MutableRecordBuffer buffer = scriptBuffer(Map.of("args", args, "defaults", defaults));
            BindContext context = ctx();

            List<ProblemSink.Problem> problems = processProblems(new PlayerArgProcessor(), buffer, context);

            assertEquals(1, problems.size());
            assertTrue(problems.get(0).message().contains("not player-resolvable"));
        }
    }

    private static Map<String, Object> withNull(String key) {
        Map<String, Object> map = new HashMap<>();
        map.put(key, null);
        return map;
    }
}
