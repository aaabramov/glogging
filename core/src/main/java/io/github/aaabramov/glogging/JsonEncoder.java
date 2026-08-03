package io.github.aaabramov.glogging;

import java.util.Map;

/**
 * Serializes a prepared log event into a single line of JSON.
 * <p>
 * The event arrives as a {@link Map} whose keys are already the exact field names Cloud
 * Logging expects — including special fields such as
 * {@code logging.googleapis.com/labels}, which no Java field name could express. An
 * implementation must therefore write the keys through <b>verbatim</b>: no renaming, no
 * naming strategy, and no treating {@code .} as a path separator. Values are strings,
 * numbers, nested maps, or small value objects with public fields.
 * <p>
 * Implementations must not throw. A layout that propagates an exception breaks the
 * application it is logging for, so serialization failures should be reported in the
 * returned string instead.
 *
 * @author Andrii Abramov
 * @since 0.2.0
 */
public interface JsonEncoder {

    /**
     * Serialize a prepared log event into JSON.
     *
     * @param event field names mapped to values, ready to write out as-is
     * @return JSON string without trailing newline
     * @author Andrii Abramov
     * @since 0.2.0
     */
    String toJson(Map<String, Object> event);

}