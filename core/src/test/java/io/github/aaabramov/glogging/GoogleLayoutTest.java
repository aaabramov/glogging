package io.github.aaabramov.glogging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GoogleLayoutTest {

    private static final String LABELS = "logging.googleapis.com/labels";

    private LoggerContext context;

    @BeforeEach
    void resetRecorder() {
        RecordingEncoder.lastEvent = null;
        context = new LoggerContext();
    }

    private GoogleLayout layout(String prefix, boolean appendLoggerName) {
        GoogleLayout layout = new GoogleLayout();
        layout.setContext(context);
        layout.setPattern("%message");
        layout.setJson(RecordingEncoder.class.getName());
        layout.setPrefix(prefix);
        layout.setAppendLoggerName(appendLoggerName);
        layout.start();
        return layout;
    }

    private LoggingEvent event(Level level, String message, Map<String, String> mdc) {
        LoggingEvent event = new LoggingEvent();
        event.setLoggerName("com.example.Foo");
        event.setLevel(level);
        event.setMessage(message);
        event.setTimeStamp(1629642099659L);
        event.setMDCPropertyMap(mdc);
        return event;
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> recordedLabels() {
        return (Map<String, String>) RecordingEncoder.lastEvent.get(LABELS);
    }

    @Test
    void mapsSeverityMessageAndTimestamp() {
        GoogleLayout layout = layout("com.acme", false);

        layout.doLayout(event(Level.WARN, "boom", Collections.emptyMap()));

        Map<String, Object> recorded = RecordingEncoder.lastEvent;
        assertEquals("WARNING", recorded.get("severity"));
        assertEquals("boom", recorded.get("message"));
        GcpTimestamp timestamp = (GcpTimestamp) recorded.get("timestamp");
        assertEquals(1629642099L, timestamp.seconds);
        assertEquals(659_000_000L, timestamp.nanos);
    }

    @Test
    void labelsGoUnderTheSpecialCollectorKey() {
        GoogleLayout layout = layout("com.acme", false);

        layout.doLayout(event(Level.INFO, "hi", Collections.singletonMap("userId", "42")));

        assertTrue(RecordingEncoder.lastEvent.containsKey(LABELS));
    }

    @Test
    void neverEmitsABareLabelsKey() {
        // The regression guard for the bug this release fixes: a top-level "labels"
        // object stays in jsonPayload and is never promoted to LogEntry.labels.
        GoogleLayout layout = layout("com.acme", false);

        layout.doLayout(event(Level.INFO, "hi", Collections.singletonMap("userId", "42")));

        assertNull(RecordingEncoder.lastEvent.get("labels"));
    }

    @Test
    void prefixesMdcEntriesAndNormalisesTrailingSlash() {
        GoogleLayout layout = layout("com.acme", false);

        layout.doLayout(event(Level.INFO, "hi", Collections.singletonMap("userId", "42")));

        assertEquals("42", recordedLabels().get("com.acme/userId"));
    }

    @Test
    void keepsSingleSlashWhenPrefixAlreadyEndsWithOne() {
        GoogleLayout layout = layout("com.acme/", false);

        layout.doLayout(event(Level.INFO, "hi", Collections.singletonMap("userId", "42")));

        assertTrue(recordedLabels().containsKey("com.acme/userId"));
    }

    @Test
    void carriesEveryMdcEntryThrough() {
        GoogleLayout layout = layout("com.acme", false);
        Map<String, String> mdc = new LinkedHashMap<>();
        mdc.put("userId", "42");
        mdc.put("requestId", "abc");
        mdc.put("tenant", "acme");

        layout.doLayout(event(Level.INFO, "hi", mdc));

        Map<String, String> labels = recordedLabels();
        assertEquals(3, labels.size());
        assertEquals("42", labels.get("com.acme/userId"));
        assertEquals("abc", labels.get("com.acme/requestId"));
        assertEquals("acme", labels.get("com.acme/tenant"));
    }

    @Test
    void appendsLoggerNameWhenEnabled() {
        GoogleLayout layout = layout("com.acme", true);

        layout.doLayout(event(Level.INFO, "hi", Collections.emptyMap()));

        assertEquals("com.example.Foo", recordedLabels().get("com.acme/loggerName"));
    }

    @Test
    void doesNotAppendLoggerNameWhenDisabled() {
        GoogleLayout layout = layout("com.acme", false);

        layout.doLayout(event(Level.INFO, "hi", Collections.emptyMap()));

        assertNull(RecordingEncoder.lastEvent.get(LABELS));
    }

    @Test
    void emptyPrefixLeavesLabelKeysUnprefixed() {
        GoogleLayout layout = layout(null, false);

        layout.doLayout(event(Level.INFO, "hi", Collections.singletonMap("userId", "42")));

        assertEquals("42", recordedLabels().get("userId"));
    }

    @Test
    void omitsLabelsWhenThereIsNoMdcAndNoLoggerName() {
        GoogleLayout layout = layout("com.acme", false);

        layout.doLayout(event(Level.INFO, "hi", Collections.emptyMap()));

        assertFalse(RecordingEncoder.lastEvent.containsKey(LABELS));
        assertEquals("[timestamp, severity, message]", RecordingEncoder.lastEvent.keySet().toString());
    }

    @Test
    void appendsANewlinePerEvent() {
        GoogleLayout layout = layout("com.acme", false);

        String out = layout.doLayout(event(Level.INFO, "hi", Collections.emptyMap()));

        assertEquals("recorded\n", out);
    }

    @Test
    void degradesToPlainMessageWhenEncoderClassIsInvalid() {
        GoogleLayout layout = new GoogleLayout();
        layout.setContext(context);
        layout.setPattern("%message");
        layout.setJson("io.github.aaabramov.glogging.DoesNotExist");
        layout.start();

        String out = layout.doLayout(event(Level.INFO, "hi", Collections.emptyMap()));

        assertEquals("hi\n", out);
        assertNull(RecordingEncoder.lastEvent);
    }

    @Test
    void degradesToPlainMessageWhenJsonParamMissing() {
        GoogleLayout layout = new GoogleLayout();
        layout.setContext(context);
        layout.setPattern("%message");
        layout.start();

        String out = layout.doLayout(event(Level.INFO, "hi", Collections.emptyMap()));

        assertEquals("hi\n", out);
    }
}
