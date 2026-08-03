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
    void aMessageThatIsItselfJsonSurvivesAsAString() {
        // Applications do log JSON. It must be escaped into the message string rather
        // than corrupting the envelope, and must come back out byte-identical.
        String userJson = "{\"orderId\":\"A-77\",\"total\":12.5,\"tags\":[\"x\",\"y\"]}";
        GcpLoggingEvent event = new GcpLoggingEvent(
                GcpTimestamp.ofEpoch(1629642099659L), "INFO", userJson, labels());

        JsonObject json = JsonParser.parseString(encoder.toJson(event.toJsonMap())).getAsJsonObject();

        assertEquals(userJson, json.get("message").getAsString());
        // The envelope must not have gained the user's keys as fields of its own.
        assertFalse(json.has("orderId"));
    }

    @Test
    void aMessageWithNewlinesAndQuotesStaysOnOneLine() {
        // Stack traces arrive here via %xException.
        String message = "boom \"quoted\"\n\tat com.example.Foo.bar(Foo.java:1)";
        GcpLoggingEvent event = new GcpLoggingEvent(
                GcpTimestamp.ofEpoch(1629642099659L), "ERROR", message, labels());

        String out = encoder.toJson(event.toJsonMap());

        assertEquals(-1, out.indexOf('\n'), "a raw newline would split one event into two log lines");
        assertEquals(message, JsonParser.parseString(out).getAsJsonObject().get("message").getAsString());
    }

    @Test
    void emitsCamelCaseFieldNamesAndNoTrailingNewline() {
        String json = encode(labels());

        // The GCP collector requires camelCase; a trailing newline is the layout's job.
        assertFalse(json.endsWith("\n"));
        assertEquals(-1, json.indexOf("logger_name"));
    }
}
