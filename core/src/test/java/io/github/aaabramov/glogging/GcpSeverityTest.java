package io.github.aaabramov.glogging;

import ch.qos.logback.classic.Level;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The expected values here are the {@code LogSeverity} enum names, verbatim.
 * Nothing else is guaranteed to be understood by the log collector.
 *
 * @see <a href="https://cloud.google.com/logging/docs/reference/v2/rest/v2/LogEntry#logseverity">LogSeverity</a>
 */
class GcpSeverityTest {

    @Test
    void errorMapsToError() {
        assertEquals("ERROR", GcpSeverity.of(Level.ERROR));
    }

    @Test
    void warnMapsToWarning() {
        // GCP spells it out in full; "WARN" is not a LogSeverity value.
        assertEquals("WARNING", GcpSeverity.of(Level.WARN));
    }

    @Test
    void infoMapsToInfo() {
        assertEquals("INFO", GcpSeverity.of(Level.INFO));
    }

    @Test
    void debugMapsToDebug() {
        assertEquals("DEBUG", GcpSeverity.of(Level.DEBUG));
    }

    @Test
    void traceMapsToDebug() {
        // GCP has no TRACE. DEBUG is the lowest real severity, so trace events
        // stay visible and filterable instead of collapsing into DEFAULT.
        assertEquals("DEBUG", GcpSeverity.of(Level.TRACE));
    }

    @Test
    void offMapsToDefault() {
        // Not reachable through a logging event, but the mapping must be total.
        assertEquals("DEFAULT", GcpSeverity.of(Level.OFF));
    }

    @Test
    void allMapsToDefault() {
        assertEquals("DEFAULT", GcpSeverity.of(Level.ALL));
    }
}
