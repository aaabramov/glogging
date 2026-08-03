package io.github.aaabramov.glogging;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * Produces 'Google Structured Logging' compatible json event.
 * <pre>
 * {@code
 * {
 *   "timestamp": {
 *     "seconds": 1629312414,
 *     "nanos": 503000000
 *   },
 *   "severity": "WARN",
 *   "message": "Formatted tests message (you define the format as usual)",
 *   "labels": {
 *     "your.custom.prefix/loggerName": "io.github.aaabramov.TestApplication",
 *     "your.custom.prefix/userId": "2223123"
 *   }
 * }
 * }
 * </pre>
 *
 * @author Andrii Abramov
 * @see <a href="https://cloud.google.com/logging/docs/structured-logging">Structured logging</a>
 * @since 0.0.1
 */
public class GoogleLayout extends PatternLayout {
    
    /**
     * Optional prefix for labels.
     */
    private String prefix;
    /**
     * JSON encoder implementation to use.
     *
     * @see JsonEncoder
     */
    private String json;
    /**
     * Whether to append logger name to labels.
     */
    private boolean appendLoggerName;
    
    public String getPrefix() {
        return prefix;
    }
    
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }
    
    public String getJson() {
        return json;
    }
    
    public void setJson(String json) {
        this.json = json;
    }
    
    public boolean isAppendLoggerName() {
        return appendLoggerName;
    }
    
    public void setAppendLoggerName(boolean appendLoggerName) {
        this.appendLoggerName = appendLoggerName;
    }
    
    private JsonEncoder jsonEncoder;

    private final List<Label> configuredLabels = new ArrayList<>();
    private Map<String, String> staticLabels = Collections.emptyMap();

    /**
     * Adds a label attached to every event. Logback calls this once per {@code <label>}
     * element in the configuration, before {@link #start()}.
     *
     * @param label the configured key/value pair
     * @since 0.3.0
     */
    public void addLabel(Label label) {
        configuredLabels.add(label);
    }

    @Override
    public void start() {
        super.start();
        if (prefix != null) {
            if (!prefix.endsWith("/")) {
                prefix = prefix + "/";
            }
        } else {
            prefix = "";
        }

        // Before the encoder check on purpose: that method returns early when <json> is
        // missing, and a label misconfiguration should still be reported when the
        // encoder is broken too.
        staticLabels = buildStaticLabels();

        if (!validateJsonEncoder()) {
            return;
        }

        try {
            jsonEncoder = ((JsonEncoder) Class.forName(json).getDeclaredConstructor().newInstance());
        } catch (Exception e) {
            reportInvalidJsonParam("Failed to initialize json encoder. Either it was not found on classpath or invalid class name provided. ");
        }
    }
    
    /**
     * Validates the configured labels and applies the prefix once, at configuration time,
     * so {@link #doLayout} does no per-event work beyond copying the result.
     */
    private Map<String, String> buildStaticLabels() {
        Map<String, String> resolved = new LinkedHashMap<>();
        for (Label label : configuredLabels) {
            String key = label.getKey() == null ? null : label.getKey().trim();
            if (key == null || key.isEmpty()) {
                addError("Ignoring <label> with a missing or blank <key>: " + label);
                continue;
            }
            if (label.getValue() == null) {
                addError("Ignoring <label> with key '" + key + "': missing <value>.");
                continue;
            }
            if (resolved.put(prefix + key, label.getValue()) != null) {
                addWarn("Duplicate <label> key '" + prefix + key + "'. The last value wins.");
            }
        }
        return resolved;
    }

    private boolean validateJsonEncoder() {
        if (json == null) {
            reportInvalidJsonParam("Missing required 'json' parameter in logback configuration. ");
            return false;
        } else if (json.isEmpty()) {
            reportInvalidJsonParam("Provided empty 'json' parameter in logback configuration. ");
            return false;
        }
        return true;
    }

    @Override
    public String doLayout(ILoggingEvent event) {
        String formattedMessage = super.doLayout(event).trim();

        // The encoder failed to initialise (misconfiguration already reported
        // to the logback status manager in start()). Degrade gracefully to the
        // plain formatted message rather than throwing an NPE on every log line.
        if (jsonEncoder == null) {
            return formattedMessage + "\n";
        }

        Map<String, String> mdc = event.getMDCPropertyMap();
        Map<String, String> labels = new HashMap<>(staticLabels.size() + mdc.size() + 1);

        // Static labels first: MDC is per-event and more specific, so it wins on a
        // key collision. A fresh copy per event - the shared map must never be mutated.
        labels.putAll(staticLabels);
        mdc.forEach((k, v) -> labels.put(prefix + k, v));
        if (appendLoggerName) {
            labels.put(prefix + "loggerName", event.getLoggerName());
        }

        GcpLoggingEvent e = new GcpLoggingEvent(
                GcpTimestamp.ofEpoch(event.getTimeStamp()),
                GcpSeverity.of(event.getLevel()),
                formattedMessage,
                labels
        );
        return jsonEncoder.toJson(e.toJsonMap()) + "\n";
    }

    private void reportInvalidJsonParam(String s) {
        addError(s + "Specify one of: [io.github.aaabramov.glogging.GsonEncoder, io.github.aaabramov.glogging.JacksonEncoder]");
    }
    
}
