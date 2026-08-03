package io.github.aaabramov.glogging;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

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
}
