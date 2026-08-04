# Logback layout for Google Structured logging

[![Maven Central](https://img.shields.io/maven-central/v/io.github.aaabramov/glogging-core?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.aaabramov/glogging-core) [![Build](https://github.com/aaabramov/glogging/actions/workflows/maven.yml/badge.svg)](https://github.com/aaabramov/glogging/actions/workflows/maven.yml)

---

[![Stand With Ukraine](https://raw.githubusercontent.com/vshymanskyy/StandWithUkraine/main/banner-direct-single.svg)](https://stand-with-ukraine.pp.ua)

---

## Why?

- Google indexes logs - easier and faster to filter logs.
  See [Using the Logs Explorer](https://cloud.google.com/logging/docs/view/logs-viewer-interface)
- Aggregating logs by labels
- Log metrics
- Proper logging levels in the UI
- See [this StackOverflow](https://stackoverflow.com/q/44164730/5091346) question

See [structured logs](https://cloud.google.com/logging/docs/structured-logging).

## Is this the right library for you?

glogging is a **small logback layout that writes structured JSON to stdout** and nothing
else. It has no opinion on how logs leave the machine: the platform's collector picks them
up, which is what already happens on GKE, Cloud Run and anywhere the Ops Agent runs.

There are other ways to get structured logs into Cloud Logging, and one of them may suit
you better:

| | Logging API | Transport | Notes |
|---|---|---|---|
| **glogging** | logback | stdout | Two small artifacts; the only third-party dependency is your choice of Gson or Jackson — or neither, if you [supply your own encoder](#custom-json-encoder). |
| [`log4j-layout-template-json`](https://logging.apache.org/log4j/2.x/manual/json-template-layout.html) | Log4j2 | stdout | Ships a `GcpLayout.json` template — verified present in 2.26.1. **If you are on Log4j2, use this**; glogging is logback-only. |
| [`google-cloud-logging-logback`](https://github.com/googleapis/java-logging-logback) | logback | Logging API | Official. Writes over the network from inside your process, so no collector is needed — useful off-GCP — at the cost of credentials, a large dependency tree, and log delivery sharing fate with your network. Still published as `-alpha` (0.144.0-alpha at the time of writing). |
| [`logstash-logback-encoder`](https://github.com/logfellow/logstash-logback-encoder) | logback | stdout | Far more featureful — structured arguments, nested payloads, many providers. No GCP template, so you configure the special field names yourself. |

Reach for glogging when you want GCP-shaped stdout logging from logback with as little
machinery as possible. Reach for one of the others when you need Log4j2, want to skip the
collector, or need richer structured payloads than `message` plus labels.

## Getting started

You need **logback 1.3 or newer** on the classpath. glogging does not bring it: it is an
extension to logback, not a distribution of it, so it declares logback as `provided` and
leaves the version to you. Most applications already have it — via `spring-boot-starter`,
say — in which case there is nothing extra to add. If yours does not, declare
`ch.qos.logback:logback-classic` alongside the artifacts below.

Maven:

```xml

<dependencies>
    <dependency>
        <groupId>io.github.aaabramov</groupId>
        <artifactId>glogging-core</artifactId>
        <version>0.2.0</version>
    </dependency>


    <dependency>
        <groupId>io.github.aaabramov</groupId>
        <artifactId>glogging-gson</artifactId>
        <!-- OR -->
        <!--<artifactId>glogging-jackson</artifactId>-->
        <version>0.2.0</version>
    </dependency>
</dependencies>
```

Gradle (Groovy):

```groovy
implementation 'io.github.aaabramov:glogging-core:0.2.0'
implementation 'io.github.aaabramov:glogging-gson:0.2.0'
// OR 
// implementation 'io.github.aaabramov:glogging-jackson:0.2.0'
```

Gradle (Kotlin):

```kotlin
implementation("io.github.aaabramov:glogging-core:0.2.0")
implementation("io.github.aaabramov:glogging-gson:0.2.0")
// OR 
// implementation("io.github.aaabramov:glogging-jackson:0.2.0")
```

Sbt:

```sbt
libraryDependencies ++= Seq(
  "io.github.aaabramov" % "glogging-core",
  "io.github.aaabramov" % "glogging-gson"
  //  OR
  //  "io.github.aaabramov" % "glogging-jackson"
).map(_ % "0.2.0")
```

## Configuration example:

```xml

<configuration>
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="ch.qos.logback.core.encoder.LayoutWrappingEncoder">
            <layout class="io.github.aaabramov.glogging.GoogleLayout">

                <!-- Which JSON encoder to use. You can also supply your own by
                     implementing the JsonEncoder interface (see below). -->
                <json>io.github.aaabramov.glogging.JacksonEncoder</json>

                <!-- OR -->
                <!-- <json>io.github.aaabramov.glogging.GsonEncoder</json> -->

                <!-- Optionally append "${prefix}/loggerName" labels -->
                <appendLoggerName>true</appendLoggerName>

                <!-- Optionally configure prefix for labels -->
                <prefix>com.yourcompany</prefix>

                <!-- Labels attached to every log line. Values go through logback's
                     ${...} substitution, so they can come from the environment. -->
                <label>
                    <key>serviceName</key>
                    <value>checkout</value>
                </label>
                <label>
                    <key>version</key>
                    <value>${APP_VERSION}</value>
                </label>

                <!-- Provide message pattern you like. -->
                <!-- Note: there is no need anymore to log timestamps & levels to the message. Google will pick them up from specific fields. -->
                <pattern>%message %xException{10}</pattern>
            </layout>
        </encoder>
    </appender>

    <appender name="ASYNCSTDOUT" class="ch.qos.logback.classic.AsyncAppender">
        <appender-ref ref="STDOUT"/>
    </appender>

    <!--  Configure logging levels  -->
    <logger name="com.github" level="DEBUG"/>

    <root level="DEBUG">
        <appender-ref ref="ASYNCSTDOUT"/>
    </root>
</configuration>
```

## Example output:

```
{"timestamp":{"seconds":1629642099,"nanos":659000000},"severity":"DEBUG","message":"debug","logging.googleapis.com/labels":{"io.github.aaabramov/name":"Andrii","io.github.aaabramov/loggerName":"io.github.aaabramov.glogging.App"}}
{"timestamp":{"seconds":1629642099,"nanos":659000000},"severity":"INFO","message":"info","logging.googleapis.com/labels":{"io.github.aaabramov/name":"Andrii","io.github.aaabramov/loggerName":"io.github.aaabramov.glogging.App"}}
{"timestamp":{"seconds":1629642099,"nanos":659000000},"severity":"WARNING","message":"warn","logging.googleapis.com/labels":{"io.github.aaabramov/name":"Andrii","io.github.aaabramov/loggerName":"io.github.aaabramov.glogging.App"}}
{"timestamp":{"seconds":1629642099,"nanos":661000000},"severity":"ERROR","message":"error java.lang.RuntimeException: BOOM\n\tat io.github.aaabramov.glogging.App.main(App.java:22)","logging.googleapis.com/labels":{"io.github.aaabramov/name":"Andrii","io.github.aaabramov/loggerName":"io.github.aaabramov.glogging.App"}}
```

## Labels

MDC entries become **`LogEntry.labels`**, which is what makes them usable for filtering
and log-based metrics. They are emitted under the special
`logging.googleapis.com/labels` key — the only key Cloud Logging promotes; a plain
`labels` object would stay buried in the payload instead.

Given `MDC.put("userId", "42")` and `<prefix>com.yourcompany</prefix>`, filter in the
Logs Explorer with:

```
labels."com.yourcompany/userId"="42"
```

Because every field glogging emits is a field Cloud Logging recognises, the entry has no
`jsonPayload` at all — the message lands in `textPayload`. Anything you added to the
message via `<pattern>`, including `%xException`, is preserved there, so Error Reporting
continues to pick up stack traces.

> **Changed in 0.2.0.** Earlier versions emitted a plain `labels` object, which was never
> promoted, so labels were only reachable as `jsonPayload.labels.*` and the message as
> `jsonPayload.message`. See [CHANGELOG.md](CHANGELOG.md) if you are upgrading.

### Labels that are the same on every line

Service name, version and region do not belong in the MDC — they never change for the
life of the process, and putting them there means setting them on every thread that
logs. Declare them on the layout instead:

```xml
<label>
    <key>serviceName</key>
    <value>checkout</value>
</label>
<label>
    <key>version</key>
    <value>${APP_VERSION}</value>
</label>
```

`<prefix>` applies to these exactly as it does to MDC keys, so with
`<prefix>com.yourcompany</prefix>` the example above is filtered as
`labels."com.yourcompany/serviceName"="checkout"`.

Values are substituted by logback before glogging sees them, so anything logback can
resolve works — `${K_REVISION}` on Cloud Run, `${HOSTNAME}` on GKE, or a system property.

If an MDC entry and a static label collide on the same key, **the MDC value wins**: a
static label is a default for the deployment, and per-event data is more specific.

A `<label>` needs both a key and a non-empty value. Logback trims element text and
collapses an empty `<value></value>` to nothing, so a label missing either one is
reported on logback's status output and skipped — it never stops the application
logging.

*Added in 0.3.0.*

### If your message is itself JSON

It stays a **string**. The message is escaped into the `message` field, so the envelope
is never corrupted and the text comes back byte-identical — but Cloud Logging does not
parse it, even when the message is nothing but JSON. You can substring-search it:

```
textPayload:"orderId"          # matches
textPayload.orderId="A-77"     # ERROR: Field not found: 'orderId'
```

That is not a regression — before 0.2.0 the message was a string in
`jsonPayload.message` and equally unparsed; only the field name changed. If you want a
value to be *queryable*, put it in the MDC so it becomes a label:

```java
MDC.put("orderId", "A-77");            // -> labels."com.yourcompany/orderId"="A-77"
```

## Severity mapping

logback levels are translated to the
[`LogSeverity`](https://cloud.google.com/logging/docs/reference/v2/rest/v2/LogEntry#logseverity)
names Cloud Logging defines, which are not spelled quite the same:

| logback | GCP severity | |
|---|---|---|
| `ERROR` | `ERROR` | |
| `WARN` | `WARNING` | GCP spells it out in full |
| `INFO` | `INFO` | |
| `DEBUG` | `DEBUG` | |
| `TRACE` | `DEBUG` | GCP has no `TRACE`; `DEBUG` is the closest real severity |

`NOTICE`, `CRITICAL`, `ALERT` and `EMERGENCY` are never emitted — logback tops out at
`ERROR`.

## Custom JSON encoder

If you would rather not add Gson or Jackson, implement `JsonEncoder` yourself and point
`<json>` at your class. It receives the event as a `Map` whose keys are already the exact
field names Cloud Logging expects, so write them out **verbatim** — no naming strategy,
and nothing that treats `.` as a path separator:

```java
package com.yourcompany;

import io.github.aaabramov.glogging.JsonEncoder;
import java.util.Map;

public class MyEncoder implements JsonEncoder {
    @Override
    public String toJson(Map<String, Object> event) {
        return yourJsonLibrary.write(event); // no trailing newline
    }
}
```

Implementations must not throw — a layout that propagates an exception breaks the
application it is logging for. Report failures in the returned string instead.

## Upgrading

**[docs/UPGRADING.md](docs/UPGRADING.md)** — migration notes for the releases that need
them. **0.2.0 is a breaking release:** it changes the JSON field names, so Logs Explorer
queries, log-based metrics and alerting policies built on the old paths need updating.

## Changelog

See **[CHANGELOG.md](CHANGELOG.md)** — worth a look before upgrading, as it records the
releases that changed the emitted JSON.

## Releasing

Published to the [Sonatype Central Portal](https://central.sonatype.com) by pushing a
version tag — the [`Release` workflow](.github/workflows/release.yml) builds, signs and
uploads the artifacts, then creates a GitHub Release:

```bash
git tag v0.1.2
git push origin v0.1.2
```

Maintainers: see **[RELEASING.md](RELEASING.md)** for the full procedure, one-time
setup, and the gotchas worth knowing before cutting a release.
