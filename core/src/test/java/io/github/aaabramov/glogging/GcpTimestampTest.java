package io.github.aaabramov.glogging;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GcpTimestampTest {

    @Test
    void splitsEpochMillisIntoSecondsAndNanos() {
        GcpTimestamp ts = GcpTimestamp.ofEpoch(1629642099659L);

        assertEquals(1629642099L, ts.seconds);
        assertEquals(659_000_000L, ts.nanos);
    }

    @Test
    void handlesEpochZero() {
        GcpTimestamp ts = GcpTimestamp.ofEpoch(0L);

        assertEquals(0L, ts.seconds);
        assertEquals(0L, ts.nanos);
    }

    @Test
    void wholeSecondHasNoNanos() {
        GcpTimestamp ts = GcpTimestamp.ofEpoch(1000L);

        assertEquals(1L, ts.seconds);
        assertEquals(0L, ts.nanos);
    }

    @Test
    void subSecondMillisBecomeNanos() {
        GcpTimestamp ts = GcpTimestamp.ofEpoch(1500L);

        assertEquals(1L, ts.seconds);
        assertEquals(500_000_000L, ts.nanos);
    }
}
