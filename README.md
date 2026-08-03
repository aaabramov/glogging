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
        <version>0.1.2</version>
    </dependency>


    <dependency>
        <groupId>io.github.aaabramov</groupId>
        <artifactId>glogging-gson</artifactId>
        <!-- OR -->
        <!--<artifactId>glogging-jackson</artifactId>-->
        <version>0.1.2</version>
    </dependency>
</dependencies>
```

Gradle (Groovy):

```groovy
implementation 'io.github.aaabramov:glogging-core:0.1.2'
implementation 'io.github.aaabramov:glogging-gson:0.1.2'
// OR 
// implementation 'io.github.aaabramov:glogging-jackson:0.1.2'
```

Gradle (Kotlin):

```kotlin
implementation("io.github.aaabramov:glogging-core:0.1.2")
implementation("io.github.aaabramov:glogging-gson:0.1.2")
// OR 
// implementation("io.github.aaabramov:glogging-jackson:0.1.2")
```

Sbt:

```sbt
libraryDependencies ++= Seq(
  "io.github.aaabramov" % "glogging-core",
  "io.github.aaabramov" % "glogging-gson"
  //  OR
  //  "io.github.aaabramov" % "glogging-jackson"
).map(_ % "0.1.2")
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

## Upgrading to 0.2.0

0.2.0 fixes labels, which had never actually worked, and the fix changes the emitted JSON.
Four things may need attention. The first two affect your **queries**, not your code, so
they will not show up as a compile error — check them before deploying.

**1. Label filters move out of the payload.** Labels are now emitted under
`logging.googleapis.com/labels`, so Cloud Logging promotes them to `LogEntry.labels`
instead of leaving them in `jsonPayload`:

```diff
- jsonPayload.labels."com.yourcompany/userId"="42"
+ labels."com.yourcompany/userId"="42"
```

**2. The message moves to `textPayload`.** Every field glogging emits is now one Cloud
Logging recognises, so it collapses the payload and there is no `jsonPayload` at all:

```diff
- jsonPayload.message:"connection refused"
+ textPayload:"connection refused"
```

Update any **log-based metrics, saved queries, alerting policies and dashboards** built on
the old paths. Stack traces are unaffected: `%xException` output still travels in the
message, so Error Reporting keeps working.

**3. Declare logback yourself if glogging was your only source of it.** logback is now
`provided`, so glogging no longer puts its own 1.3.x on your classpath — where it competed
with whatever logback you actually run:

```xml
<dependency>
    <groupId>ch.qos.logback</groupId>
    <artifactId>logback-classic</artifactId>
    <version>1.5.x</version> <!-- or 1.3.x on Java 8 -->
</dependency>
```

Most applications already get logback via `spring-boot-starter` or similar and need no
change — those also stop being at risk of a silent downgrade to 1.3.x.

**4. Only if you wrote your own `JsonEncoder`:** the method now takes a
`Map<String, Object>` instead of the (package-private, hence unusable) event type. See
[Custom JSON encoder](#custom-json-encoder). If you use `GsonEncoder` or `JacksonEncoder`,
there is nothing to do.

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
