package io.github.aaabramov.glogging;

import ch.qos.logback.classic.Level;

/**
 * Translates a logback {@link Level} into a Google Cloud Logging {@code LogSeverity}.
 * <p>
 * The two vocabularies are close but not identical: logback's {@code WARN} is spelled
 * {@code WARNING} by GCP, and GCP has no {@code TRACE} at all. Sending a name that is
 * not in the enum leaves the collector guessing - it does try to match common severity
 * strings, but that leniency is not part of any contract, and an unmatched value lands
 * as {@code DEFAULT}, which sorts below {@code DEBUG} and is easy to miss in the Logs
 * Explorer. Emitting the canonical names removes the guesswork.
 * <p>
 * GCP severities with no logback equivalent ({@code NOTICE}, {@code CRITICAL},
 * {@code ALERT}, {@code EMERGENCY}) are unreachable from here - logback tops out at
 * {@code ERROR}.
 *
 * @author Andrii Abramov
 * @see <a href="https://cloud.google.com/logging/docs/reference/v2/rest/v2/LogEntry#logseverity">LogSeverity</a>
 * @since 0.1.2
 */
class GcpSeverity {

    private GcpSeverity() {
    }

    /**
     * @param level logback level of the event
     * @return the matching {@code LogSeverity} name, or {@code DEFAULT} if there is none
     */
    static String of(Level level) {
        switch (level.toInt()) {
            case Level.ERROR_INT:
                return "ERROR";
            case Level.WARN_INT:
                return "WARNING";
            case Level.INFO_INT:
                return "INFO";
            case Level.DEBUG_INT:
                // TRACE has no GCP counterpart; DEBUG is the closest real severity,
                // so trace events stay filterable rather than collapsing to DEFAULT.
            case Level.TRACE_INT:
                return "DEBUG";
            default:
                // Only OFF and ALL reach this, neither of which can carry an event.
                return "DEFAULT";
        }
    }

}
