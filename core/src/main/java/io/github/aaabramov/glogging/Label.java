package io.github.aaabramov.glogging;

/**
 * A label declared in the logback configuration and attached to every log event.
 * <pre>
 * {@code
 * <label>
 *     <key>serviceName</key>
 *     <value>checkout</value>
 * </label>
 * }
 * </pre>
 * Logback's configuration engine instantiates this reflectively, which is why it is a
 * public class with a public no-argument constructor and plain setters. Values are
 * subject to logback's {@code ${...}} substitution before they reach us, so
 * {@code <value>${APP_VERSION}</value>} works without any support from this library.
 *
 * @author Andrii Abramov
 * @see GoogleLayout#addLabel(Label)
 * @since 0.3.0
 */
public class Label {

    private String key;
    private String value;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "Label[key=" + key + ", value=" + value + "]";
    }
}
