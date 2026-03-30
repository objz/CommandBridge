package dev.objz.commandbridge.scripting.validation.processor;

import dev.objz.commandbridge.TestFixtures;
import dev.objz.commandbridge.scripting.anno.Default;
import dev.objz.commandbridge.scripting.anno.Max;
import dev.objz.commandbridge.scripting.anno.Min;
import dev.objz.commandbridge.scripting.anno.Pattern;
import dev.objz.commandbridge.scripting.anno.Required;
import dev.objz.commandbridge.scripting.bind.BindContext;
import dev.objz.commandbridge.scripting.bind.ConvertContext;
import dev.objz.commandbridge.scripting.bind.RecordBinder;
import dev.objz.commandbridge.scripting.bind.TypeAdapter;
import dev.objz.commandbridge.scripting.bind.TypeAdapterRegistry;
import dev.objz.commandbridge.scripting.model.Script;
import dev.objz.commandbridge.scripting.model.enums.ArgType;
import dev.objz.commandbridge.scripting.model.records.mapping.ArgMapping;
import dev.objz.commandbridge.scripting.model.records.mapping.CmdMapping;
import dev.objz.commandbridge.scripting.platform.PlatformFeatures;
import dev.objz.commandbridge.scripting.validation.ProblemSink;
import dev.objz.commandbridge.scripting.yaml.YamlNode;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for validation processors.
 * Verifies field constraint enforcement for @Required, @Min, @Max, @Default, @Pattern,
 * argument ordering, and trailing argument rules.
 */
final class ValidationProcessorsTest {

    @BeforeAll
    static void setUp() {
        TestFixtures.ensureLog();
    }

    record RequiredModel(@Required String name, String optional) { }

    record MinModel(@Min(5) int count) { }

    record MaxModel(@Max(100) int count) { }

    record DefaultModel(@Default("hello") String greeting) { }

    record PatternModel(@Pattern(regex = "^[a-z]+$") String value) { }

    record PatternOnIntModel(@Pattern(regex = "^\\d+$") int value) { }

    private static RecordBinder.MutableRecordBuffer bufferOf(Class<?> recordClass, Object... values) {
        return new RecordBinder.MutableRecordBuffer(
                recordClass, recordClass.getSimpleName(),
                recordClass.getRecordComponents(), values);
    }

    private static BindContext contextWith(ProblemSink problems) {
        return new BindContext(new TypeAdapterRegistry(), problems, PlatformFeatures.none());
    }

    private static BindContext contextWithStringAdapter(ProblemSink problems) {
        var adapters = new TypeAdapterRegistry();
        adapters.register(new TypeAdapter<String>() {
            @Override
            public boolean supports(Type targetType) {
                return targetType == String.class;
            }

            @Override
            public String fromYaml(YamlNode node, Type targetType, ConvertContext ctx) {
                if (node instanceof YamlNode.Scalar s) {
                    return String.valueOf(s.value());
                }
                return null;
            }

            @Override
            public YamlNode toYaml(String value, Type targetType, ConvertContext ctx) {
                return YamlNode.scalar(value);
            }
        });
        return new BindContext(adapters, problems, PlatformFeatures.none());
    }

    private static RecordBinder.MutableRecordBuffer scriptBuffer(List<ArgMapping> args,
            List<CmdMapping> commands) {
        var comps = Script.class.getRecordComponents();
        Object[] values = new Object[comps.length];
        for (int i = 0; i < comps.length; i++) {
            if ("args".equals(comps[i].getName())) {
                values[i] = args;
            } else if ("commands".equals(comps[i].getName())) {
                values[i] = commands;
            }
        }
        return new RecordBinder.MutableRecordBuffer(Script.class, "script", comps, values);
    }

    @Nested
    final class RequiredProcessorTests {

        private final RequiredProcessor processor = new RequiredProcessor();

        @Test
        void nullRequiredFieldAddsError() {
            var problems = new ProblemSink();
            var buf = bufferOf(RequiredModel.class, null, "opt");
            processor.process(buf, contextWith(problems));
            assertTrue(problems.hasErrors());
        }

        @Test
        void nonNullRequiredFieldPassesValidation() {
            var problems = new ProblemSink();
            var buf = bufferOf(RequiredModel.class, "present", null);
            processor.process(buf, contextWith(problems));
            assertFalse(problems.hasErrors());
        }
    }

    @Nested
    final class MinProcessorTests {

        private final MinProcessor processor = new MinProcessor();

        @Test
        void valueBelowMinAddsError() {
            var problems = new ProblemSink();
            var buf = bufferOf(MinModel.class, 3);
            processor.process(buf, contextWith(problems));
            assertTrue(problems.hasErrors());
        }

        @Test
        void valueAtMinPassesValidation() {
            var problems = new ProblemSink();
            var buf = bufferOf(MinModel.class, 5);
            processor.process(buf, contextWith(problems));
            assertFalse(problems.hasErrors());
        }

        @Test
        void nullValueSkipsValidation() {
            var problems = new ProblemSink();
            var buf = bufferOf(MinModel.class, (Object) null);
            processor.process(buf, contextWith(problems));
            assertFalse(problems.hasErrors());
        }
    }

    @Nested
    final class MaxProcessorTests {

        private final MaxProcessor processor = new MaxProcessor();

        @Test
        void valueAboveMaxAddsError() {
            var problems = new ProblemSink();
            var buf = bufferOf(MaxModel.class, 101);
            processor.process(buf, contextWith(problems));
            assertTrue(problems.hasErrors());
        }

        @Test
        void valueAtMaxPassesValidation() {
            var problems = new ProblemSink();
            var buf = bufferOf(MaxModel.class, 100);
            processor.process(buf, contextWith(problems));
            assertFalse(problems.hasErrors());
        }

        @Test
        void nullValueSkipsValidation() {
            var problems = new ProblemSink();
            var buf = bufferOf(MaxModel.class, (Object) null);
            processor.process(buf, contextWith(problems));
            assertFalse(problems.hasErrors());
        }
    }

    @Nested
    final class DefaultProcessorTests {

        private final DefaultProcessor processor = new DefaultProcessor();

        @Test
        void nullFieldWithDefaultAppliesDefault() {
            var problems = new ProblemSink();
            var buf = bufferOf(DefaultModel.class, (Object) null);
            processor.process(buf, contextWithStringAdapter(problems));
            assertEquals("hello", buf.get(0));
        }

        @Test
        void nonNullFieldWithDefaultKeepsOriginalValue() {
            var problems = new ProblemSink();
            var buf = bufferOf(DefaultModel.class, "world");
            processor.process(buf, contextWith(problems));
            assertEquals("world", buf.get(0));
        }
    }

    @Nested
    final class PatternProcessorTests {

        private final PatternProcessor processor = new PatternProcessor();

        @Test
        void valueMatchingRegexPassesValidation() {
            var problems = new ProblemSink();
            var buf = bufferOf(PatternModel.class, "abc");
            processor.process(buf, contextWith(problems));
            assertFalse(problems.hasErrors());
        }

        @Test
        void valueNotMatchingRegexAddsError() {
            var problems = new ProblemSink();
            var buf = bufferOf(PatternModel.class, "ABC123");
            processor.process(buf, contextWith(problems));
            assertTrue(problems.hasErrors());
        }

        @Test
        void nonStringValueWithPatternAddsError() {
            var problems = new ProblemSink();
            var buf = bufferOf(PatternOnIntModel.class, 42);
            processor.process(buf, contextWith(problems));
            assertTrue(problems.hasErrors());
        }
    }

    @Nested
    final class ArgumentOrderProcessorTests {

        private final ArgumentOrderProcessor processor = new ArgumentOrderProcessor();

        @Test
        void validArgumentOrderPassesValidation() {
            var problems = new ProblemSink();
            var reqArg = new ArgMapping("reqArg", true, ArgType.STRING, null);
            var optArg = new ArgMapping("optArg", false, ArgType.STRING, null);
            var cmd = new CmdMapping("say ${reqArg} ${optArg}", null, null, null, null, null);
            var buf = scriptBuffer(List.of(reqArg, optArg), List.of(cmd));
            processor.process(buf, contextWith(problems));
            assertFalse(problems.hasErrors());
        }

        @Test
        void requiredAfterOptionalAddsError() {
            var problems = new ProblemSink();
            var reqArg = new ArgMapping("reqArg", true, ArgType.STRING, null);
            var optArg = new ArgMapping("optArg", false, ArgType.STRING, null);
            var cmd = new CmdMapping("say ${optArg} ${reqArg}", null, null, null, null, null);
            var buf = scriptBuffer(List.of(reqArg, optArg), List.of(cmd));
            processor.process(buf, contextWith(problems));
            assertTrue(problems.hasErrors());
        }
    }

    @Nested
    final class TrailingArgumentProcessorTests {

        private final TrailingArgumentProcessor processor = new TrailingArgumentProcessor();

        @Test
        void greedyStringAsLastArgumentPassesValidation() {
            var problems = new ProblemSink();
            var normalArg = new ArgMapping("normalArg", false, ArgType.STRING, null);
            var greedyArg = new ArgMapping("greedyArg", false, ArgType.GREEDY_STRING, null);
            var cmd = new CmdMapping("say ${normalArg} ${greedyArg}", null, null, null, null, null);
            var buf = scriptBuffer(List.of(normalArg, greedyArg), List.of(cmd));
            processor.process(buf, contextWith(problems));
            assertFalse(problems.hasErrors());
        }

        @Test
        void greedyStringNotLastAddsError() {
            var problems = new ProblemSink();
            var greedyArg = new ArgMapping("greedyArg", false, ArgType.GREEDY_STRING, null);
            var normalArg = new ArgMapping("normalArg", false, ArgType.STRING, null);
            var cmd = new CmdMapping("say ${greedyArg} ${normalArg}", null, null, null, null, null);
            var buf = scriptBuffer(List.of(greedyArg, normalArg), List.of(cmd));
            processor.process(buf, contextWith(problems));
            assertTrue(problems.hasErrors());
        }
    }
}
