package io.github.aaabramov.glogging;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Represents logging event to be picked up by GCP logging collector.
 *
 * @author Andrii Abramov
 * @since 0.0.1
 */
class GcpLoggingEvent {

    /**
     * The collector only promotes labels to {@code LogEntry.labels} under this exact key.
     * A plain {@code "labels"} object is not a special field: it stays inside
     * {@code jsonPayload}, where it cannot be used for label-based filtering or log
     * metrics. Confirmed empirically against a GKE cluster, not just from the docs.
     */
    static final String LABELS_KEY = "logging.googleapis.com/labels";

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
    
    Map<String, Object> toJsonMap() {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("timestamp", timestamp);
        json.put("severity", severity);
        json.put("message", message);
        if (!labels.isEmpty()) {
            json.put(LABELS_KEY, labels);
        }
        return json;
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
