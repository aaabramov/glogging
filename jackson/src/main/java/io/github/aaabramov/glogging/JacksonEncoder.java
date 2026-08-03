package io.github.aaabramov.glogging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;


import java.util.Map;

/**
 * Encode a prepared log event using the Jackson library.
 * <p>
 * A default {@link ObjectMapper} is deliberate: it writes map keys verbatim, which is
 * what the special {@code logging.googleapis.com/*} field names require. Do not add a
 * {@code PropertyNamingStrategy} here.
 *
 * @author Andrii Abramov
 * @since 0.0.1
 */
public class JacksonEncoder implements JsonEncoder {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String toJson(Map<String, Object> event) {
        try {
            return this.objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            return "Error occurred during event serialization using jackson: " + e + ". Original event: " + event;
        }
    }

}