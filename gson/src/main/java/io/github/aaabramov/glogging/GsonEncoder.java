package io.github.aaabramov.glogging;

import com.google.gson.Gson;

import java.util.Map;

/**
 * Encode a prepared log event using the Gson library.
 * <p>
 * A default {@link Gson} is deliberate: it writes map keys verbatim, which is what the
 * special {@code logging.googleapis.com/*} field names require. Do not add a
 * {@code FieldNamingPolicy} here.
 *
 * @author Andrii Abramov
 * @since 0.0.1
 */
public class GsonEncoder implements JsonEncoder {

    private final Gson gson = new Gson();

    @Override
    public String toJson(Map<String, Object> event) {
        return this.gson.toJson(event);
    }

}