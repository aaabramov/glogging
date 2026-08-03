package io.github.aaabramov.glogging;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A malformed {@code <label>} must never stop the application logging. Problems are
 * reported to logback's status manager, which is where a user actually meets them.
 */
class StaticLabelsValidationTest {

    private static boolean anyContains(List<String> messages, String fragment) {
        for (String message : messages) {
            if (message.contains(fragment)) {
                return true;
            }
        }
        return false;
    }

    @Test
    void aBlankKeyIsReportedAndSkippedWhileSiblingsSurvive() {
        LogbackFixture fixture = LogbackFixture.withLayout(
                "<label><key>   </key><value>ignored</value></label>\n"
                        + "<label><key>serviceName</key><value>checkout</value></label>\n");

        fixture.log("hello");

        assertTrue(anyContains(fixture.errors(), "blank <key>"),
                "expected an error about the blank key, got: " + fixture.errors());
        assertEquals(1, fixture.labels().size());
        assertEquals("checkout", fixture.labels().get("serviceName"));
    }

    @Test
    void aMissingKeyElementIsReportedAndSkipped() {
        LogbackFixture fixture = LogbackFixture.withLayout(
                "<label><value>orphaned</value></label>\n");

        fixture.log("hello");

        assertTrue(anyContains(fixture.errors(), "blank <key>"),
                "expected an error, got: " + fixture.errors());
        assertFalse(fixture.lastEvent().containsKey(LogbackFixture.LABELS),
                "the only label was invalid, so no labels key is emitted");
    }

    @Test
    void aMissingValueElementIsReportedAndSkipped() {
        LogbackFixture fixture = LogbackFixture.withLayout(
                "<label><key>serviceName</key></label>\n");

        fixture.log("hello");

        assertTrue(anyContains(fixture.errors(), "missing <value>"),
                "expected an error, got: " + fixture.errors());
        assertFalse(fixture.lastEvent().containsKey(LogbackFixture.LABELS));
    }

    @Test
    void anEmptyValueElementIsReportedTheSameWayAsAMissingOne() {
        // logback collapses <value></value> to null, so the two cases are the same input
        // and must produce the same diagnosis.
        LogbackFixture fixture = LogbackFixture.withLayout(
                "<label><key>serviceName</key><value></value></label>\n");

        fixture.log("hello");

        assertTrue(anyContains(fixture.errors(), "missing <value>"),
                "expected an error, got: " + fixture.errors());
        assertFalse(fixture.lastEvent().containsKey(LogbackFixture.LABELS));
    }

    @Test
    void aDuplicateKeyWarnsAndTheLastValueWins() {
        LogbackFixture fixture = LogbackFixture.withLayout(
                "<label><key>serviceName</key><value>first</value></label>\n"
                        + "<label><key>serviceName</key><value>second</value></label>\n");

        fixture.log("hello");

        assertTrue(anyContains(fixture.warnings(), "Duplicate <label> key 'serviceName'"),
                "expected a duplicate-key warning, got: " + fixture.warnings());
        assertEquals("second", fixture.labels().get("serviceName"));
        assertEquals(1, fixture.labels().size());
    }

    @Test
    void aDuplicateKeyIsDetectedAfterPrefixing() {
        LogbackFixture fixture = LogbackFixture.withLayout(
                "<prefix>com.acme</prefix>\n"
                        + "<label><key>serviceName</key><value>first</value></label>\n"
                        + "<label><key>serviceName</key><value>second</value></label>\n");

        fixture.log("hello");

        assertTrue(anyContains(fixture.warnings(), "Duplicate <label> key 'com.acme/serviceName'"),
                "the warning should name the emitted key, got: " + fixture.warnings());
    }

    @Test
    void aMalformedLabelNeverStopsLogging() {
        LogbackFixture fixture = LogbackFixture.withLayout(
                "<label><key></key><value></value></label>\n");

        fixture.log("still logging");

        assertEquals("still logging", fixture.lastEvent().get("message"));
        assertEquals("INFO", fixture.lastEvent().get("severity"));
    }
}
