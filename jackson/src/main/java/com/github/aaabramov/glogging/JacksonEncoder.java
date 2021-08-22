package com.github.aaabramov.glogging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * Encode {@link GcpLoggingEvent} using Jackson library.
 */
class JacksonEncoder implements JsonEncoder {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public String toJson(GcpLoggingEvent event) {
        try {
            return this.objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            return "Error occurred during event serialization using jackson: " + e + ". Original event: " + event;
        }
    }
    
}