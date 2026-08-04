package io.github.aaabramov.glogging;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.util.LogbackMDCAdapter;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.status.Status;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Drives {@link GoogleLayout} through a real logback XML configuration.
 * <p>
 * Tests that call {@code layout.addLabel(...)} directly from Java would pass even if
 * Joran could not wire the element at all — the configuration contract is convention
 * based and nothing checks it at compile time. So the contract is pinned here instead.
 */
final class LogbackFixture {

    static final String LABELS = "logging.googleapis.com/labels";

    private final LoggerContext context = new LoggerContext();

    private LogbackFixture(String xml) {
        // Without an MDC adapter every append throws inside logback with
        // "Cannot invoke MDCAdapter.getCopyOfContextMap() because mdcAdapter is null",
        // which logback swallows into the StatusManager - so the encoder is never
        // called and the test sees nothing recorded, with no visible cause.
        context.setMDCAdapter(new LogbackMDCAdapter());

        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(context);
        try {
            configurator.doConfigure(new ByteArrayInputStream(toUtf8(xml)));
        } catch (JoranException e) {
            throw new AssertionError("logback rejected the configuration:\n" + xml, e);
        }
        RecordingEncoder.lastEvent = null;
    }

    private static byte[] toUtf8(String s) {
        try {
            return s.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Wraps {@code layoutBody} — extra elements such as {@code <label>} or
     * {@code <prefix>} — in a complete configuration using {@link RecordingEncoder}.
     */
    static LogbackFixture withLayout(String layoutBody) {
        return new LogbackFixture(
                "<configuration>\n"
                        + "  <appender name='STDOUT' class='ch.qos.logback.core.ConsoleAppender'>\n"
                        + "    <encoder class='ch.qos.logback.core.encoder.LayoutWrappingEncoder'>\n"
                        + "      <layout class='io.github.aaabramov.glogging.GoogleLayout'>\n"
                        + "        <json>io.github.aaabramov.glogging.RecordingEncoder</json>\n"
                        + "        <pattern>%message</pattern>\n"
                        + layoutBody
                        + "      </layout>\n"
                        + "    </encoder>\n"
                        + "  </appender>\n"
                        + "  <root level='DEBUG'><appender-ref ref='STDOUT'/></root>\n"
                        + "</configuration>\n");
    }

    /** For tests that need control over the whole document, e.g. {@code <property>}. */
    static LogbackFixture withConfiguration(String xml) {
        return new LogbackFixture(xml);
    }

    /** Always goes through the context's own adapter; the static slf4j MDC is a different instance. */
    void mdc(String key, String value) {
        context.getMDCAdapter().put(key, value);
    }

    void clearMdc() {
        context.getMDCAdapter().clear();
    }

    void log(String message) {
        context.getLogger("com.example.Foo").info(message);
    }

    Map<String, Object> lastEvent() {
        return RecordingEncoder.lastEvent;
    }

    @SuppressWarnings("unchecked")
    Map<String, String> labels() {
        Map<String, Object> event = RecordingEncoder.lastEvent;
        if (event == null) {
            throw new AssertionError("nothing was recorded - the appender probably failed; "
                    + "status was: " + allStatuses());
        }
        return (Map<String, String>) event.get(LABELS);
    }

    List<String> errors() {
        return statusMessages(Status.ERROR);
    }

    List<String> warnings() {
        return statusMessages(Status.WARN);
    }

    private List<String> statusMessages(int level) {
        List<String> messages = new ArrayList<>();
        for (Status status : context.getStatusManager().getCopyOfStatusList()) {
            if (status.getLevel() == level) {
                messages.add(status.getMessage());
            }
        }
        return messages;
    }

    private List<String> allStatuses() {
        List<String> messages = new ArrayList<>();
        for (Status status : context.getStatusManager().getCopyOfStatusList()) {
            messages.add(status.toString());
        }
        return messages;
    }
}
