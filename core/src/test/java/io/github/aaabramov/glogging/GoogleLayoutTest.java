package io.github.aaabramov.glogging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GoogleLayoutTest {

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

    @Test
    void mapsSeverityMessageAndTimestamp() {
        GoogleLayout layout = layout("com.acme", false);

        layout.doLayout(event(Level.WARN, "boom", Collections.emptyMap()));

        GcpLoggingEvent recorded = RecordingEncoder.lastEvent;
        assertEquals("WARNING", recorded.severity);
        assertEquals("boom", recorded.message);
        assertEquals(1629642099L, recorded.timestamp.seconds);
        assertEquals(659_000_000L, recorded.timestamp.nanos);
    }

    @Test
    void prefixesMdcEntriesAndNormalisesTrailingSlash() {
        GoogleLayout layout = layout("com.acme", false);

        layout.doLayout(event(Level.INFO, "hi", Collections.singletonMap("userId", "42")));

        Map<String, String> labels = RecordingEncoder.lastEvent.labels;
        assertEquals("42", labels.get("com.acme/userId"));
    }

    @Test
    void keepsSingleSlashWhenPrefixAlreadyEndsWithOne() {
        GoogleLayout layout = layout("com.acme/", false);

        layout.doLayout(event(Level.INFO, "hi", Collections.singletonMap("userId", "42")));

        assertTrue(RecordingEncoder.lastEvent.labels.containsKey("com.acme/userId"));
    }

    @Test
    void appendsLoggerNameWhenEnabled() {
        GoogleLayout layout = layout("com.acme", true);

        layout.doLayout(event(Level.INFO, "hi", Collections.emptyMap()));

        assertEquals("com.example.Foo", RecordingEncoder.lastEvent.labels.get("com.acme/loggerName"));
    }

    @Test
    void doesNotAppendLoggerNameWhenDisabled() {
        GoogleLayout layout = layout("com.acme", false);

        layout.doLayout(event(Level.INFO, "hi", Collections.emptyMap()));

        assertFalse(RecordingEncoder.lastEvent.labels.containsKey("com.acme/loggerName"));
    }

    @Test
    void emptyPrefixLeavesLabelKeysUnprefixed() {
        GoogleLayout layout = layout(null, false);

        layout.doLayout(event(Level.INFO, "hi", Collections.singletonMap("userId", "42")));

        assertEquals("42", RecordingEncoder.lastEvent.labels.get("userId"));
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
