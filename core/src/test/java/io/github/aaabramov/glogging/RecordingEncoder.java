package io.github.aaabramov.glogging;

/**
 * Test {@link JsonEncoder} that records the last event it was asked to encode,
 * so {@link GoogleLayoutTest} can assert on the layout's output without pulling
 * in gson or jackson.
 */
public class RecordingEncoder implements JsonEncoder {

    static volatile GcpLoggingEvent lastEvent;

    @Override
    public String toJson(GcpLoggingEvent event) {
        lastEvent = event;
        return "recorded";
    }
}
