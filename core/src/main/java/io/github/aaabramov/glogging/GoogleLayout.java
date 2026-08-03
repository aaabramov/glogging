package io.github.aaabramov.glogging;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.HashMap;
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
        
        if (!validateJsonEncoder()) {
            return;
        }

        try {
            jsonEncoder = ((JsonEncoder) Class.forName(json).getDeclaredConstructor().newInstance());
        } catch (Exception e) {
            reportInvalidJsonParam("Failed to initialize json encoder. Either it was not found on classpath or invalid class name provided. ");
        }
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
        Map<String, String> labels = new HashMap<>(mdc.size() + 1);

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
