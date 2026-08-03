package io.github.aaabramov.glogging;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StaticLabelsConfigTest {

    @Test
    void aConfigurationWithNoLabelsBehavesExactlyAsBefore() {
        LogbackFixture fixture = LogbackFixture.withLayout("");

        fixture.log("hello");

        assertEquals("INFO", fixture.lastEvent().get("severity"));
        assertEquals("hello", fixture.lastEvent().get("message"));
        assertFalse(fixture.lastEvent().containsKey(LogbackFixture.LABELS),
                "no MDC and no labels means the labels key is omitted entirely");
    }

    @Test
    void mdcStillBecomesLabelsThroughRealXml() {
        LogbackFixture fixture = LogbackFixture.withLayout("<prefix>com.acme</prefix>\n");

        fixture.mdc("userId", "42");
        fixture.log("hello");

        assertEquals("42", fixture.labels().get("com.acme/userId"));
    }

    @Test
    void emitsAStaticLabelUnderTheSpecialCollectorKey() {
        LogbackFixture fixture = LogbackFixture.withLayout(
                "<label><key>serviceName</key><value>checkout</value></label>\n");

        fixture.log("hello");

        assertEquals("checkout", fixture.labels().get("serviceName"));
    }

    @Test
    void severalLabelElementsAllAccumulate() {
        // Proves Joran's adder convention fires once per element, rather than the
        // last <label> replacing the previous one.
        LogbackFixture fixture = LogbackFixture.withLayout(
                "<label><key>serviceName</key><value>checkout</value></label>\n"
                        + "<label><key>region</key><value>europe-west1</value></label>\n"
                        + "<label><key>tier</key><value>web</value></label>\n");

        fixture.log("hello");

        assertEquals(3, fixture.labels().size());
        assertEquals("checkout", fixture.labels().get("serviceName"));
        assertEquals("europe-west1", fixture.labels().get("region"));
        assertEquals("web", fixture.labels().get("tier"));
    }

    @Test
    void thePrefixAppliesToStaticLabelsJustLikeMdc() {
        LogbackFixture fixture = LogbackFixture.withLayout(
                "<prefix>com.acme</prefix>\n"
                        + "<label><key>serviceName</key><value>checkout</value></label>\n");

        fixture.log("hello");

        assertEquals("checkout", fixture.labels().get("com.acme/serviceName"));
    }

    @Test
    void withoutAPrefixStaticLabelKeysAreVerbatim() {
        LogbackFixture fixture = LogbackFixture.withLayout(
                "<label><key>serviceName</key><value>checkout</value></label>\n");

        fixture.log("hello");

        assertTrue(fixture.labels().containsKey("serviceName"));
    }

    @Test
    void logbackTrimsElementTextSoSurroundingWhitespaceNeverSurvives() {
        // Measured against logback 1.3.16: Joran trims element text before the layout
        // sees it, for keys and values alike. glogging cannot preserve it and does not
        // try; its own key trim matters only for programmatic addLabel calls.
        LogbackFixture fixture = LogbackFixture.withLayout(
                "<label><key>  serviceName  </key><value>  checkout  </value></label>\n");

        fixture.log("hello");

        assertEquals("checkout", fixture.labels().get("serviceName"));
    }

    @Test
    void anEmptyValueElementIsRejectedJustLikeAMissingOne() {
        // logback collapses <value></value> to null, which is indistinguishable from no
        // <value> at all - so both are rejected rather than one silently becoming an
        // empty label.
        LogbackFixture fixture = LogbackFixture.withLayout(
                "<label><key>serviceName</key><value></value></label>\n");

        fixture.log("hello");

        assertFalse(fixture.lastEvent().containsKey(LogbackFixture.LABELS),
                "the only label was invalid, so no labels key is emitted");
    }

    @Test
    void aWhitespaceOnlyValueIsRejectedToo() {
        LogbackFixture fixture = LogbackFixture.withLayout(
                "<label><key>serviceName</key><value>   </value></label>\n");

        fixture.log("hello");

        assertFalse(fixture.lastEvent().containsKey(LogbackFixture.LABELS));
    }

    @Test
    void labelValuesGetJoranVariableSubstitution() {
        // The headline use case: labels sourced from the environment. Joran substitutes
        // before our code sees the value, so this costs us nothing.
        LogbackFixture fixture = LogbackFixture.withConfiguration(
                "<configuration>\n"
                        + "  <property name='APP_VERSION' value='1.4.2'/>\n"
                        + "  <appender name='STDOUT' class='ch.qos.logback.core.ConsoleAppender'>\n"
                        + "    <encoder class='ch.qos.logback.core.encoder.LayoutWrappingEncoder'>\n"
                        + "      <layout class='io.github.aaabramov.glogging.GoogleLayout'>\n"
                        + "        <json>io.github.aaabramov.glogging.RecordingEncoder</json>\n"
                        + "        <pattern>%message</pattern>\n"
                        + "        <label><key>version</key><value>${APP_VERSION}</value></label>\n"
                        + "      </layout>\n"
                        + "    </encoder>\n"
                        + "  </appender>\n"
                        + "  <root level='DEBUG'><appender-ref ref='STDOUT'/></root>\n"
                        + "</configuration>\n");

        fixture.log("hello");

        assertEquals("1.4.2", fixture.labels().get("version"));
    }

    @Test
    void labelValuesCanComeFromASystemProperty() {
        System.setProperty("glogging.test.revision", "abc123");
        try {
            LogbackFixture fixture = LogbackFixture.withLayout(
                    "<label><key>revision</key><value>${glogging.test.revision}</value></label>\n");

            fixture.log("hello");

            assertEquals("abc123", fixture.labels().get("revision"));
        } finally {
            System.clearProperty("glogging.test.revision");
        }
    }
}
