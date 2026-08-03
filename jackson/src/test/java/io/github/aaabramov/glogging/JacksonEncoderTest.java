package io.github.aaabramov.glogging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JacksonEncoderTest {

    private static final String LABELS = "logging.googleapis.com/labels";

    private final JacksonEncoder encoder = new JacksonEncoder();
    private final ObjectMapper mapper = new ObjectMapper();

    private GcpLoggingEvent sampleEvent(Map<String, String> labels) {
        return new GcpLoggingEvent(
                GcpTimestamp.ofEpoch(1629642099659L),
                "ERROR",
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

    private JsonNode parse(Map<String, String> labels) throws Exception {
        return mapper.readTree(encode(labels));
    }

    @Test
    void producesGcpStructuredJson() throws Exception {
        JsonNode json = parse(labels());

        assertEquals("ERROR", json.get("severity").asText());
        assertEquals("hello world", json.get("message").asText());
        assertEquals(1629642099L, json.get("timestamp").get("seconds").asLong());
        assertEquals(659_000_000L, json.get("timestamp").get("nanos").asLong());
    }

    @Test
    void labelsAreEmittedUnderTheSpecialCollectorKey() throws Exception {
        JsonNode json = parse(labels());

        assertEquals("Andrii", json.get(LABELS).get("io.github.aaabramov/name").asText());
    }

    @Test
    void doesNotEmitABareLabelsKey() throws Exception {
        JsonNode json = parse(labels());

        assertNull(json.get("labels"), "a bare 'labels' object is not promoted to LogEntry.labels");
    }

    @Test
    void theSpecialKeyAppearsLiterallyInTheOutput() {
        // Jackson must not escape or rewrite the dots and slash; the collector matches
        // the key byte for byte.
        String json = encode(labels());

        assertTrue(json.contains("\"" + LABELS + "\":"), "actual output was: " + json);
    }

    @Test
    void doesNotSplitTheDottedKeyIntoNestedObjects() throws Exception {
        // Jackson has features that treat '.' as a path separator; confirm none is on by
        // default for map keys, which would give {"logging":{"googleapis":{...}}}.
        JsonNode json = parse(labels());

        assertNull(json.get("logging"));
        assertTrue(json.has(LABELS));
    }

    @Test
    void omitsLabelsWhenThereAreNone() throws Exception {
        JsonNode json = parse(Collections.emptyMap());

        assertFalse(json.has(LABELS));
        assertFalse(json.has("labels"));
    }

    @Test
    void aMessageThatIsItselfJsonSurvivesAsAString() throws Exception {
        // Applications do log JSON. It must be escaped into the message string rather
        // than corrupting the envelope, and must come back out byte-identical.
        String userJson = "{\"orderId\":\"A-77\",\"total\":12.5,\"tags\":[\"x\",\"y\"]}";
        GcpLoggingEvent event = new GcpLoggingEvent(
                GcpTimestamp.ofEpoch(1629642099659L), "INFO", userJson, labels());

        JsonNode json = mapper.readTree(encoder.toJson(event.toJsonMap()));

        assertEquals(userJson, json.get("message").asText());
        // The envelope must not have gained the user's keys as fields of its own.
        assertFalse(json.has("orderId"));
    }

    @Test
    void aMessageWithNewlinesAndQuotesStaysOnOneLine() throws Exception {
        // Stack traces arrive here via %xException.
        String message = "boom \"quoted\"\n\tat com.example.Foo.bar(Foo.java:1)";
        GcpLoggingEvent event = new GcpLoggingEvent(
                GcpTimestamp.ofEpoch(1629642099659L), "ERROR", message, labels());

        String out = encoder.toJson(event.toJsonMap());

        assertEquals(-1, out.indexOf('\n'), "a raw newline would split one event into two log lines");
        assertEquals(message, mapper.readTree(out).get("message").asText());
    }

    @Test
    void emitsCamelCaseFieldNamesAndNoTrailingNewline() {
        String json = encode(labels());

        assertFalse(json.endsWith("\n"));
        assertEquals(-1, json.indexOf("logger_name"));
    }

    @Test
    void reportsSerializationFailureInsteadOfThrowing() {
        // A logging layout must never propagate an exception into the caller's code path.
        Map<String, Object> unserializable = Collections.singletonMap("boom", new Object() {
            @SuppressWarnings("unused")
            public String getValue() {
                throw new IllegalStateException("nope");
            }
        });

        String json = encoder.toJson(unserializable);

        assertTrue(json.startsWith("Error occurred during event serialization using jackson"),
                "actual output was: " + json);
    }
}
