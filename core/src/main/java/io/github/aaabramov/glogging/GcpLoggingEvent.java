package io.github.aaabramov.glogging;

import java.util.Map;
import java.util.StringJoiner;

/**
 * Represents logging event to be picked up by GCP logging collector.
 *
 * @author Andrii Abramov
 * @since 0.0.1
 */
class GcpLoggingEvent {
    
    public final GcpTimestamp timestamp;
    public final String severity;
    public final String message;
    public final Map<String, String> labels;
    
    GcpLoggingEvent(
            GcpTimestamp timestamp,
            String severity,
            String message,
            Map<String, String> labels
    ) {
        this.timestamp = timestamp;
        this.severity = severity;
        this.message = message;
        this.labels = labels;
    }
    
    @Override
    public String toString() {
        return new StringJoiner(", ", GcpLoggingEvent.class.getSimpleName() + "[", "]")
                .add("timestamp=" + timestamp)
                .add("severity='" + severity + "'")
                .add("message='" + message + "'")
                .add("labels=" + labels)
                .toString();
    }
}
