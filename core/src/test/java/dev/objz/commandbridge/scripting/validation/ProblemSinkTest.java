package dev.objz.commandbridge.scripting.validation;

import dev.objz.commandbridge.logging.Log;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProblemSinkTest {

    @BeforeAll
    static void installLog() {
        try {
            Log.install(LoggerFactory.getLogger("test"));
        } catch (IllegalStateException e) {
            // Log already installed, ignore
        }
    }

    @Test
    void errorAddsToList() {
        ProblemSink sink = new ProblemSink();
        sink.error("field", "msg");
        assertEquals(1, sink.count());
    }

    @Test
    void hasErrorsTrueAfterError() {
        ProblemSink sink = new ProblemSink();
        sink.error("field", "msg");
        assertTrue(sink.hasErrors());
    }

    @Test
    void hasErrorsFalseWhenEmpty() {
        ProblemSink sink = new ProblemSink();
        assertFalse(sink.hasErrors());
    }

    @Test
    void countReturnsCorrectNumber() {
        ProblemSink sink = new ProblemSink();
        sink.error("field1", "msg1");
        sink.error("field2", "msg2");
        sink.error("field3", "msg3");
        assertEquals(3, sink.count());
    }

    @Test
    void problemsReturnsImmutableCopy() {
        ProblemSink sink = new ProblemSink();
        sink.error("field", "msg");
        List<ProblemSink.Problem> problems = sink.problems();
        assertEquals(1, problems.size());
        // Verify it's a copy by trying to modify it
        try {
            problems.add(new ProblemSink.Problem("field2", "msg2"));
            // If we get here, the list is mutable (test should fail)
            assertTrue(false, "problems() should return an immutable copy");
        } catch (UnsupportedOperationException e) {
            // Expected: the returned list is immutable
            assertTrue(true);
        }
    }

    @Test
    void toBulletedListFormatsWithHeader() {
        ProblemSink sink = new ProblemSink();
        sink.error("field", "msg");
        String output = sink.toBulletedList("Errors");
        assertTrue(output.contains("Errors"));
        assertTrue(output.contains("  - field: msg"));
    }

    @Test
    void toBulletedListWithNullPath() {
        ProblemSink sink = new ProblemSink();
        sink.error(null, "msg");
        String output = sink.toBulletedList("Errors");
        assertTrue(output.contains("  - msg"));
        assertFalse(output.contains("null:"));
    }

    @Test
    void toStringFormatsAllProblems() {
        ProblemSink sink = new ProblemSink();
        sink.error("field", "msg");
        String output = sink.toString();
        assertTrue(output.contains("field: msg"));
    }

    @Test
    void problemRecordToString() {
        ProblemSink.Problem withPath = new ProblemSink.Problem("field", "msg");
        assertEquals("field: msg", withPath.toString());

        ProblemSink.Problem withoutPath = new ProblemSink.Problem(null, "msg");
        assertEquals("msg", withoutPath.toString());
    }
}
