package io.github.aaabramov.glogging;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.util.LogbackMDCAdapter;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Drives the whole pipeline — logback XML, GoogleLayout, GsonEncoder, ConsoleAppender —
 * and asserts on the bytes that reach stdout, which is what the collector reads.
 */
class GsonStdoutIntegrationTest {

    private static final String LABELS = "logging.googleapis.com/labels";

    private PrintStream originalOut;
    private ByteArrayOutputStream captured;

    @BeforeEach
    void captureStdout() throws UnsupportedEncodingException {
        originalOut = System.out;
        captured = new ByteArrayOutputStream();
        // Must happen before doConfigure: ConsoleAppender resolves System.out at start().
        System.setOut(new PrintStream(captured, true, "UTF-8"));
    }

    @AfterEach
    void restoreStdout() {
        System.setOut(originalOut);
    }

    private LoggerContext configure(String layoutBody) {
        String xml = "<configuration>\n"
                + "  <appender name='STDOUT' class='ch.qos.logback.core.ConsoleAppender'>\n"
                + "    <encoder class='ch.qos.logback.core.encoder.LayoutWrappingEncoder'>\n"
                + "      <layout class='io.github.aaabramov.glogging.GoogleLayout'>\n"
                + "        <json>io.github.aaabramov.glogging.GsonEncoder</json>\n"
                + "        <pattern>%message</pattern>\n"
                + layoutBody
                + "      </layout>\n"
                + "    </encoder>\n"
                + "  </appender>\n"
                + "  <root level='DEBUG'><appender-ref ref='STDOUT'/></root>\n"
                + "</configuration>\n";
        LoggerContext context = new LoggerContext();
        // A hand-built context has no MDC adapter, and every append then throws inside
        // logback where the status manager swallows it.
        context.setMDCAdapter(new LogbackMDCAdapter());
        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(context);
        try {
            configurator.doConfigure(new ByteArrayInputStream(xml.getBytes("UTF-8")));
        } catch (Exception e) {
            throw new AssertionError("logback rejected the configuration:\n" + xml, e);
        }
        return context;
    }

    private String[] capturedLines() {
        try {
            String text = captured.toString("UTF-8");
            return text.isEmpty() ? new String[0] : text.split("\n", -1);
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError(e);
        }
    }

    private JsonObject firstLineAsJson() {
        String[] lines = capturedLines();
        assertTrue(lines.length > 0 && !lines[0].isEmpty(), "nothing was written to stdout");
        return JsonParser.parseString(lines[0]).getAsJsonObject();
    }

    @Test
    void aStaticLabelReachesStdoutAsJson() {
        configure("<label><key>serviceName</key><value>checkout</value></label>\n")
                .getLogger("com.example.Foo").info("hello");

        JsonObject json = firstLineAsJson();

        assertEquals("hello", json.get("message").getAsString());
        assertEquals("checkout", json.getAsJsonObject(LABELS).get("serviceName").getAsString());
    }

    @Test
    void aDottedStaticLabelKeySurvivesVerbatim() {
        // A serializer treating '.' as a path separator, or escaping the slash, would
        // break label filtering in the Logs Explorer.
        configure("<prefix>com.acme</prefix>\n"
                + "<label><key>service.name</key><value>checkout</value></label>\n")
                .getLogger("com.example.Foo").info("hello");

        String line = capturedLines()[0];

        assertTrue(line.contains("\"com.acme/service.name\":\"checkout\""),
                "actual output was: " + line);
    }

    @Test
    void aStaticLabelValueNeedingEscapingRoundTrips() {
        configure("<label><key>note</key><value>he said \"boom\" — ok</value></label>\n")
                .getLogger("com.example.Foo").info("hello");

        String[] lines = capturedLines();
        JsonObject json = JsonParser.parseString(lines[0]).getAsJsonObject();

        assertEquals("he said \"boom\" — ok",
                json.getAsJsonObject(LABELS).get("note").getAsString());
        assertEquals("", lines[1], "one event must occupy exactly one line");
    }

    @Test
    void aStaticLabelSurvivesAlongsideAStackTrace() {
        // %xException renders a multi-line stack trace into the message; the envelope
        // must stay on one line and keep the labels intact.
        configure("<label><key>serviceName</key><value>checkout</value></label>\n")
                .getLogger("com.example.Foo").error("boom", new RuntimeException("BOOM"));

        String[] lines = capturedLines();
        JsonObject json = JsonParser.parseString(lines[0]).getAsJsonObject();

        assertEquals("checkout", json.getAsJsonObject(LABELS).get("serviceName").getAsString());
        assertEquals("ERROR", json.get("severity").getAsString());
        assertEquals("", lines[1], "a stack trace must not split the event across lines");
    }

    @Test
    void eachEventIsOneParseableLine() {
        LoggerContext context = configure(
                "<label><key>serviceName</key><value>checkout</value></label>\n");
        context.getLogger("com.example.Foo").info("one");
        context.getLogger("com.example.Foo").warn("two");
        context.getLogger("com.example.Foo").error("three");

        String[] lines = capturedLines();

        assertEquals(4, lines.length, "three events plus the trailing empty string");
        assertEquals("INFO", JsonParser.parseString(lines[0]).getAsJsonObject().get("severity").getAsString());
        assertEquals("WARNING", JsonParser.parseString(lines[1]).getAsJsonObject().get("severity").getAsString());
        assertEquals("ERROR", JsonParser.parseString(lines[2]).getAsJsonObject().get("severity").getAsString());
        for (int i = 0; i < 3; i++) {
            assertEquals("checkout", JsonParser.parseString(lines[i]).getAsJsonObject()
                    .getAsJsonObject(LABELS).get("serviceName").getAsString());
        }
    }

    @Test
    void mdcAndStaticLabelsArriveTogetherOnStdout() {
        LoggerContext context = configure("<prefix>com.acme</prefix>\n"
                + "<label><key>serviceName</key><value>checkout</value></label>\n");
        context.getMDCAdapter().put("userId", "42");
        context.getLogger("com.example.Foo").info("hello");

        JsonObject labels = firstLineAsJson().getAsJsonObject(LABELS);

        assertEquals("checkout", labels.get("com.acme/serviceName").getAsString());
        assertEquals("42", labels.get("com.acme/userId").getAsString());
    }
}
