package dev.objz.commandbridge.velocity.cmd.bridge.types;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TimeParserTest {

    @Test
    void readsTicksSecondsAndDays() throws Exception {
        assertEquals(20, TimeParser.ticks("20"));
        assertEquals(20, TimeParser.ticks("1s"));
        assertEquals(24000, TimeParser.ticks("1d"));
        assertEquals(5, TimeParser.ticks("5T"));
    }

    @Test
    void refusesWhatIsNotATime() {
        for (String bad : new String[] { "", " ", "1m", "abc", "-5", "s" })
            assertThrows(IllegalArgumentException.class, () -> TimeParser.ticks(bad), bad);
    }

    @Test
    void aTimeTooLargeIsRefusedNotWrappedAround() {
        assertThrows(IllegalArgumentException.class, () -> TimeParser.ticks("100000d"));
        assertThrows(IllegalArgumentException.class, () -> TimeParser.ticks("200000000s"));
        assertThrows(IllegalArgumentException.class, () -> TimeParser.ticks("99999999999"));
    }
}
