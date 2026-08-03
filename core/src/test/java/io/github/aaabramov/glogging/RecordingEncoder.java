package io.github.aaabramov.glogging;

import java.util.Map;

/**
 * Test {@link JsonEncoder} that records the last wire-format map it was asked to encode,
 * so {@link GoogleLayoutTest} can assert on the layout's output — including the exact
 * JSON keys — without pulling in gson or jackson.
 */
public class RecordingEncoder implements JsonEncoder {

    static volatile Map<String, Object> lastEvent;

    @Override
    public String toJson(Map<String, Object> event) {
        lastEvent = event;
        return "recorded";
    }
}
