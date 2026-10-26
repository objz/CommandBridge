package dev.objz.commandbridge.scripting.bind.adapters;

import dev.objz.commandbridge.scripting.bind.BindContext;
import dev.objz.commandbridge.scripting.bind.RecordBinder;
import dev.objz.commandbridge.scripting.bind.TypeAdapterRegistry;
import dev.objz.commandbridge.scripting.platform.PlatformFeatures;
import dev.objz.commandbridge.scripting.validation.ProblemSink;
import dev.objz.commandbridge.scripting.yaml.YamlNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ListAdapterTest {

    record Holder(List<String> strings, List<Integer> ints) {
    }

    private static final Type LIST_OF_STRING = Holder.class.getRecordComponents()[0].getGenericType();
    private static final Type LIST_OF_INT = Holder.class.getRecordComponents()[1].getGenericType();

    private final ProblemSink problems = new ProblemSink();
    private final BindContext ctx = new BindContext(new TypeAdapterRegistry()
            .register(new StringAdapter())
            .register(new PrimitivesAdapter()), problems, PlatformFeatures.none());

    private final ListAdapter adapter = new ListAdapter();

    @AfterEach
    void clearPath() {
        RecordBinder.setCurrentPath(null);
    }

    @Test
    void supportsOnlyParameterizedLists() {
        assertTrue(adapter.supports(LIST_OF_STRING));
        assertFalse(adapter.supports(List.class));
        assertFalse(adapter.supports(String.class));
    }

    @Test
    void convertsEachElementWithElementAdapter() {
        var node = YamlNode.sequence(List.of(YamlNode.scalar("a"), YamlNode.scalar(2)));

        assertEquals(List.of("a", "2"), adapter.fromYaml(node, LIST_OF_STRING, ctx));
        assertFalse(problems.hasErrors());
    }

    @Test
    void emptySequenceGivesEmptyList() {
        assertEquals(List.of(), adapter.fromYaml(YamlNode.sequence(List.of()), LIST_OF_STRING, ctx));
    }

    @Test
    void nonSequenceIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> adapter.fromYaml(YamlNode.scalar("a"), LIST_OF_STRING, ctx));
    }

    @Test
    void failingElementIsReportedWithIndexAndKeptAsNull() {
        RecordBinder.setCurrentPath("script.aliases");
        var node = YamlNode.sequence(List.of(
                YamlNode.scalar("ok"), YamlNode.mapping(Map.of("k", YamlNode.scalar("v")))));

        List<?> out = adapter.fromYaml(node, LIST_OF_STRING, ctx);

        assertEquals(Arrays.asList("ok", null), out);
        assertEquals(1, problems.count());
        assertEquals("script.aliases[1]", problems.problems().getFirst().path());
        assertEquals("script.aliases", RecordBinder.currentPath());
    }

    @Test
    void failingElementWithoutPathUsesListPrefix() {
        var node = YamlNode.sequence(List.of(YamlNode.scalar("x")));

        adapter.fromYaml(node, LIST_OF_INT, ctx);

        assertEquals("list[0]", problems.problems().getFirst().path());
    }

    @Test
    void toYamlConvertsEachElement() {
        assertEquals(YamlNode.sequence(List.of(YamlNode.scalar("a"), YamlNode.scalar("b"))),
                adapter.toYaml(List.of("a", "b"), LIST_OF_STRING, ctx));
        assertEquals(YamlNode.sequence(List.of()), adapter.toYaml(null, LIST_OF_STRING, ctx));
    }
}
