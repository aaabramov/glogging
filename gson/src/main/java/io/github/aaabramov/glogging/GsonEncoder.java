package io.github.aaabramov.glogging;

import com.google.gson.Gson;

/**
 * Encode {@link GcpLoggingEvent} using Gson library.
 *
 * @author Andrii Abramov
 * @since 0.0.1
 */
public class GsonEncoder implements JsonEncoder {
    
    private final Gson gson = new Gson();
    
    @Override
    public String toJson(GcpLoggingEvent event) {
        return this.gson.toJson(event);
    }
    
}