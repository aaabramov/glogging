package io.github.aaabramov.glogging;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class GsonEncoderTest {

    private final GsonEncoder encoder = new GsonEncoder();

    private GcpLoggingEvent sampleEvent() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("io.github.aaabramov/name", "Andrii");
        return new GcpLoggingEvent(
                GcpTimestamp.ofEpoch(1629642099659L),
                "INFO",
                "hello world",
                labels
        );
    }

    @Test
    void producesGcpStructuredJson() {
        JsonObject json = JsonParser.parseString(encoder.toJson(sampleEvent())).getAsJsonObject();

        assertEquals("INFO", json.get("severity").getAsString());
        assertEquals("hello world", json.get("message").getAsString());

        JsonObject timestamp = json.getAsJsonObject("timestamp");
        assertEquals(1629642099L, timestamp.get("seconds").getAsLong());
        assertEquals(659_000_000L, timestamp.get("nanos").getAsLong());

        assertEquals("Andrii", json.getAsJsonObject("labels").get("io.github.aaabramov/name").getAsString());
    }

    @Test
    void emitsCamelCaseFieldNamesAndNoTrailingNewline() {
        String json = encoder.toJson(sampleEvent());

        // The GCP collector requires camelCase; a trailing newline is the layout's job, not the encoder's.
        assertFalse(json.endsWith("\n"));
        assertEquals(-1, json.indexOf("logger_name"));
    }
}
