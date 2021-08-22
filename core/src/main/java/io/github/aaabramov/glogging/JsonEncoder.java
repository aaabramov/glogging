package io.github.aaabramov.glogging;

/**
 * Serializes {@link GcpLoggingEvent}s into JSON.
 *
 * @author Andrii Abramov
 * @since 0.0.1
 */
public interface JsonEncoder {
    
    /**
     * Serialize {@link GcpLoggingEvent}s into JSON.
     *
     * @param event packaged logging event to serialize
     * @return JSON string without trailing newline
     * @implSpec camelCase <b>must</b> be used.
     * @author Andrii Abramov
     * @since 0.0.1
     */
    String toJson(GcpLoggingEvent event);
    
}