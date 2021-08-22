package io.github.aaabramov.glogging;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.status.ErrorStatus;
import ch.qos.logback.core.status.StatusManager;

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
 *     "your.custom.prefix/loggerName": "com.github.aaabramov.TestApplication",
 *     "your.custom.prefix/userId": "2223123"
 *   }
 * }
 * }
 * </pre>
 *
 * @see <a href="https://cloud.google.com/logging/docs/structured-logging">Structured logging</a>
 */
public class GoogleLayout extends PatternLayout {
    
    /**
     * Optional prefix for labels.
     */
    private String prefix;
    /**
     * JSON encoder implementation to use.
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
        
        validateJsonEncoder();
        
        try {
            jsonEncoder = ((JsonEncoder) Class.forName(json).newInstance());
        } catch (Exception e) {
            reportInvalidJsonParam("Failed to initialize json encoder. Either it was not found on classpath or invalid class name provided.");
        }
    }
    
    private void validateJsonEncoder() {
        if (json == null) {
            reportInvalidJsonParam("Missing required 'json' parameter in logback configuration.");
        } else if (json.isEmpty()) {
            reportInvalidJsonParam("Provided empty 'json' parameter in logback configuration.");
        }
    }
    
    @Override
    public String doLayout(ILoggingEvent event) {
        String formattedMessage = super.doLayout(event).trim();
        
        Map<String, String> mdc = event.getMDCPropertyMap();
        Map<String, String> labels = new HashMap<>(mdc.size() + 1);
        
        mdc.forEach((k, v) -> labels.put(prefix + k, v));
        if (appendLoggerName) {
            labels.put(prefix + "loggerName", event.getLoggerName());
        }
        
        GcpLoggingEvent e = new GcpLoggingEvent(
                GcpTimestamp.ofEpoch(event.getTimeStamp()),
                event.getLevel().levelStr,
                formattedMessage,
                labels
        );
        return jsonEncoder.toJson(e) + "\n";
    }
    
    private void reportInvalidJsonParam(String s) {
        StatusManager sm = this.getContext().getStatusManager();
        sm.add(
                new ErrorStatus(
                        s + "Specify on of: [com.github.aaabramov.glogging.GsonEncoder, TODO]",
                        this
                )
        );
    }
    
}
