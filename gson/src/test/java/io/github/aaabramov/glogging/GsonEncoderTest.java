package io.github.aaabramov.glogging;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GsonEncoderTest {

    private static final String LABELS = "logging.googleapis.com/labels";

    private final GsonEncoder encoder = new GsonEncoder();

    private GcpLoggingEvent sampleEvent(Map<String, String> labels) {
        return new GcpLoggingEvent(
                GcpTimestamp.ofEpoch(1629642099659L),
                "INFO",
                "hello world",
                labels
        );
    }

    private Map<String, String> labels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("io.github.aaabramov/name", "Andrii");
        return labels;
    }

    private String encode(Map<String, String> labels) {
        return encoder.toJson(sampleEvent(labels).toJsonMap());
    }

    private JsonObject parse(Map<String, String> labels) {
        return JsonParser.parseString(encode(labels)).getAsJsonObject();
    }

    @Test
    void producesGcpStructuredJson() {
        JsonObject json = parse(labels());

        assertEquals("INFO", json.get("severity").getAsString());
        assertEquals("hello world", json.get("message").getAsString());

        JsonObject timestamp = json.getAsJsonObject("timestamp");
        assertEquals(1629642099L, timestamp.get("seconds").getAsLong());
        assertEquals(659_000_000L, timestamp.get("nanos").getAsLong());
    }

    @Test
    void labelsAreEmittedUnderTheSpecialCollectorKey() {
        JsonObject json = parse(labels());

        assertEquals("Andrii", json.getAsJsonObject(LABELS).get("io.github.aaabramov/name").getAsString());
    }

    @Test
    void doesNotEmitABareLabelsKey() {
        JsonObject json = parse(labels());

        assertNull(json.get("labels"), "a bare 'labels' object is not promoted to LogEntry.labels");
    }

    @Test
    void theSpecialKeyAppearsLiterallyInTheOutput() {
        // Gson must not escape or otherwise rewrite the dots and slash; the collector
        // matches the key byte for byte.
        String json = encode(labels());

        assertTrue(json.contains("\"" + LABELS + "\":"), "actual output was: " + json);
    }

    @Test
    void doesNotSplitTheDottedKeyIntoNestedObjects() {
        // A serializer treating '.' as a path separator would produce
        // {"logging":{"googleapis":{"com/labels":...}}} instead of a flat key.
        JsonObject json = parse(labels());

        assertNull(json.get("logging"));
        assertTrue(json.has(LABELS));
    }

    @Test
    void omitsLabelsWhenThereAreNone() {
        JsonObject json = parse(Collections.emptyMap());

        assertFalse(json.has(LABELS));
        assertFalse(json.has("labels"));
    }

    @Test
    void emitsCamelCaseFieldNamesAndNoTrailingNewline() {
        String json = encode(labels());

        // The GCP collector requires camelCase; a trailing newline is the layout's job.
        assertFalse(json.endsWith("\n"));
        assertEquals(-1, json.indexOf("logger_name"));
    }
}
