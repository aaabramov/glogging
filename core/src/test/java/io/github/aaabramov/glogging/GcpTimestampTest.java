package io.github.aaabramov.glogging;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    // Pre-1970 instants. Unreachable from a logging event on a sane clock, but the
    // protobuf Timestamp these fields model requires 0 <= nanos <= 999,999,999, so a
    // negative nanos value is not merely odd — it is invalid, and the collector is under
    // no obligation to make sense of it.

    @Test
    void negativeMillisBorrowFromSecondsRatherThanGoingNegative() {
        GcpTimestamp ts = GcpTimestamp.ofEpoch(-1500L);

        // -1.5s == -2s + 0.5s
        assertEquals(-2L, ts.seconds);
        assertEquals(500_000_000L, ts.nanos);
    }

    @Test
    void oneMillisecondBeforeEpoch() {
        GcpTimestamp ts = GcpTimestamp.ofEpoch(-1L);

        assertEquals(-1L, ts.seconds);
        assertEquals(999_000_000L, ts.nanos);
    }

    @Test
    void wholeSecondBeforeEpochHasNoNanos() {
        GcpTimestamp ts = GcpTimestamp.ofEpoch(-1000L);

        assertEquals(-1L, ts.seconds);
        assertEquals(0L, ts.nanos);
    }

    @Test
    void nanosAreNeverNegativeAndNeverOverflowASecond() {
        for (long millis : new long[]{-2500L, -1001L, -1000L, -999L, -1L, 0L, 1L, 999L, 1000L, 1001L}) {
            GcpTimestamp ts = GcpTimestamp.ofEpoch(millis);

            assertTrue(ts.nanos >= 0, "nanos must not be negative for millis=" + millis + ", got " + ts.nanos);
            assertTrue(ts.nanos < 1_000_000_000L, "nanos must be under one second for millis=" + millis);
        }
    }

    @Test
    void secondsAndNanosAlwaysReconstructTheOriginalInstant() {
        for (long millis : new long[]{-2500L, -1500L, -1L, 0L, 1L, 1500L, 1629642099659L}) {
            GcpTimestamp ts = GcpTimestamp.ofEpoch(millis);

            assertEquals(millis, ts.seconds * 1000L + ts.nanos / 1_000_000L,
                    "round-trip failed for millis=" + millis);
        }
    }
}
