package io.github.aaabramov.glogging;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The wire format is a contract with the Cloud Logging collector, so these tests assert
 * exact key strings rather than anything derived — a typo in a special field name is
 * silently ignored by the collector, which is the failure mode this whole class exists
 * to prevent.
 *
 * @see <a href="https://cloud.google.com/logging/docs/structured-logging">Structured logging</a>
 */
class GcpLoggingEventTest {

    private static final String LABELS = "logging.googleapis.com/labels";

    private GcpLoggingEvent event(Map<String, String> labels) {
        return new GcpLoggingEvent(
                GcpTimestamp.ofEpoch(1629642099659L),
                "WARNING",
                "boom",
                labels
        );
    }

    private Map<String, String> oneLabel() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("com.acme/userId", "42");
        return labels;
    }

    @Test
    void labelsUseTheSpecialCollectorKey() {
        Map<String, Object> json = event(oneLabel()).toJsonMap();

        assertTrue(json.containsKey(LABELS), "labels must use the logging.googleapis.com/ prefix");
    }

    @Test
    void doesNotEmitABareLabelsKey() {
        // Regression guard for the actual bug: a top-level "labels" object is NOT a
        // special field, so the collector leaves it in jsonPayload instead of promoting
        // it to LogEntry.labels. Verified against a real GKE cluster.
        Map<String, Object> json = event(oneLabel()).toJsonMap();

        assertFalse(json.containsKey("labels"), "a bare 'labels' key is not promoted by the collector");
    }

    @Test
    void labelMapIsCarriedThroughUnchanged() {
        Map<String, String> labels = oneLabel();

        Map<String, Object> json = event(labels).toJsonMap();

        assertSame(labels, json.get(LABELS));
    }

    @Test
    void omitsLabelsEntirelyWhenThereAreNone() {
        Map<String, Object> json = event(Collections.emptyMap()).toJsonMap();

        assertFalse(json.containsKey(LABELS), "an empty label map should not be emitted at all");
    }

    @Test
    void emitsSeverityMessageAndTimestamp() {
        Map<String, Object> json = event(Collections.emptyMap()).toJsonMap();

        assertEquals("WARNING", json.get("severity"));
        assertEquals("boom", json.get("message"));
        assertTrue(json.get("timestamp") instanceof GcpTimestamp);
    }

    @Test
    void emitsExactlyTheExpectedKeysWithLabels() {
        Map<String, Object> json = event(oneLabel()).toJsonMap();

        // Any extra key would end up in the collector's payload unrecognised.
        assertEquals("[timestamp, severity, message, " + LABELS + "]", json.keySet().toString());
    }

    @Test
    void emitsExactlyTheExpectedKeysWithoutLabels() {
        Map<String, Object> json = event(Collections.emptyMap()).toJsonMap();

        assertEquals("[timestamp, severity, message]", json.keySet().toString());
    }

    @Test
    void keyOrderIsStable() {
        // Not required by the collector, but stable output keeps logs diffable and these
        // tests honest about ordering.
        Map<String, Object> first = event(oneLabel()).toJsonMap();
        Map<String, Object> second = event(oneLabel()).toJsonMap();

        assertEquals(first.keySet().toString(), second.keySet().toString());
    }
}
