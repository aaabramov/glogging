package io.github.aaabramov.glogging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class JacksonEncoderTest {

    private final JacksonEncoder encoder = new JacksonEncoder();
    private final ObjectMapper mapper = new ObjectMapper();

    private GcpLoggingEvent sampleEvent() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("io.github.aaabramov/name", "Andrii");
        return new GcpLoggingEvent(
                GcpTimestamp.ofEpoch(1629642099659L),
                "ERROR",
                "hello world",
                labels
        );
    }

    @Test
    void producesGcpStructuredJson() throws Exception {
        JsonNode json = mapper.readTree(encoder.toJson(sampleEvent()));

        assertEquals("ERROR", json.get("severity").asText());
        assertEquals("hello world", json.get("message").asText());
        assertEquals(1629642099L, json.get("timestamp").get("seconds").asLong());
        assertEquals(659_000_000L, json.get("timestamp").get("nanos").asLong());
        assertEquals("Andrii", json.get("labels").get("io.github.aaabramov/name").asText());
    }

    @Test
    void emitsCamelCaseFieldNamesAndNoTrailingNewline() {
        String json = encoder.toJson(sampleEvent());

        assertFalse(json.endsWith("\n"));
        assertEquals(-1, json.indexOf("logger_name"));
    }
}
