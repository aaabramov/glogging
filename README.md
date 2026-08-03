# Logback layout for Google Structured logging

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.aaabramov/glogging/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.github.aaabramov/glogging) ![Build](https://github.com/aaabramov/glogging/actions/workflows/maven.yml/badge.svg)

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

## Getting started

Maven:

```xml

<dependencies>
    <dependency>
        <groupId>io.github.aaabramov</groupId>
        <artifactId>glogging-core</artifactId>
        <version>0.1.0</version>
    </dependency>


    <dependency>
        <groupId>io.github.aaabramov</groupId>
        <artifactId>glogging-gson</artifactId>
        <!-- OR -->
        <!--<artifactId>glogging-jackson</artifactId>-->
        <version>0.1.0</version>
    </dependency>
</dependencies>
```

Gradle (Groovy):

```groovy
implementation 'io.github.aaabramov:glogging-core:0.1.0'
implementation 'io.github.aaabramov:glogging-gson:0.1.0'
// OR 
// implementation 'io.github.aaabramov:glogging-jackson:0.1.0'
```

Gradle (Kotlin):

```kotlin
implementation("io.github.aaabramov:glogging-core:0.1.0")
implementation("io.github.aaabramov:glogging-gson:0.1.0")
// OR 
// implementation("io.github.aaabramov:glogging-jackson:0.1.0")
```

Sbt:

```sbt
libraryDependencies ++= Seq(
  "io.github.aaabramov" % "glogging-core",
  "io.github.aaabramov" % "glogging-gson"
  //  OR
  //  "io.github.aaabramov" % "glogging-jackson"
).map(_ % "0.1.0")
```

## Configuration example:

```xml

<configuration>
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="ch.qos.logback.core.encoder.LayoutWrappingEncoder">
            <layout class="io.github.aaabramov.glogging.GoogleLayout">

                <!-- You have a choice which JSON encoder to use. Or create your own via implementing JsonEncoder interface -->
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
{"timestamp":{"seconds":1629642099,"nanos":659000000},"severity":"DEBUG","message":"debug","labels":{"io.github.aaabramov/name":"Andrii","io.github.aaabramov/loggerName":"io.github.aaabramov.glogging.App"}}
{"timestamp":{"seconds":1629642099,"nanos":659000000},"severity":"INFO","message":"info","labels":{"io.github.aaabramov/name":"Andrii","io.github.aaabramov/loggerName":"io.github.aaabramov.glogging.App"}}
{"timestamp":{"seconds":1629642099,"nanos":659000000},"severity":"WARN","message":"warn","labels":{"io.github.aaabramov/name":"Andrii","io.github.aaabramov/loggerName":"io.github.aaabramov.glogging.App"}}
{"timestamp":{"seconds":1629642099,"nanos":661000000},"severity":"ERROR","message":"error java.lang.RuntimeException: BOOM\n\tat io.github.aaabramov.glogging.App.main(App.java:22)","labels":{"io.github.aaabramov/name":"Andrii","io.github.aaabramov/loggerName":"io.github.aaabramov.glogging.App"}}
{"timestamp":{"seconds":1629642102,"nanos":661000000},"severity":"DEBUG","message":"debug","labels":{"io.github.aaabramov/name":"Andrii","io.github.aaabramov/loggerName":"io.github.aaabramov.glogging.App"}}
{"timestamp":{"seconds":1629642102,"nanos":661000000},"severity":"INFO","message":"info","labels":{"io.github.aaabramov/name":"Andrii","io.github.aaabramov/loggerName":"io.github.aaabramov.glogging.App"}}
{"timestamp":{"seconds":1629642102,"nanos":661000000},"severity":"WARN","message":"warn","labels":{"io.github.aaabramov/name":"Andrii","io.github.aaabramov/loggerName":"io.github.aaabramov.glogging.App"}}
{"timestamp":{"seconds":1629642102,"nanos":661000000},"severity":"ERROR","message":"error","labels":{"io.github.aaabramov/name":"Andrii","io.github.aaabramov/loggerName":"io.github.aaabramov.glogging.App"}}
```

## Releasing

Published to the [Sonatype Central Portal](https://central.sonatype.com) (the legacy
OSSRH / `s01.oss.sonatype.org` staging flow was [retired on 2025-06-30](https://central.sonatype.org/news/20250326_ossrh_sunset/)).
The `release` profile attaches sources+javadoc, GPG-signs, and uploads to the Central
Portal with `autoPublish=true`, so there is no manual "Close & Release" step.

### Via GitHub Actions (recommended)

The [`Release` workflow](.github/workflows/release.yml) publishes from CI, so no local
token or GPG key is needed. Configure these repository secrets once
(**Settings → Secrets and variables → Actions**):

| Secret | Value |
| --- | --- |
| `CENTRAL_TOKEN_USERNAME` | Central Portal user token username ([central.sonatype.com/usertoken](https://central.sonatype.com/usertoken)) |
| `CENTRAL_TOKEN_PASSWORD` | Central Portal user token password |
| `GPG_PRIVATE_KEY` | ASCII-armored secret key: `gpg --armor --export-secret-keys <KEY_ID>` |
| `GPG_PASSPHRASE` | passphrase for that key |

Then release by pushing a version tag:

```bash
git tag v0.1.1
git push origin v0.1.1
```

The tag name (minus the `v`) becomes the released version. You can also trigger it
manually from the **Actions** tab ("Run workflow") with an explicit version.

### Locally

Needs a Central Portal user token in `~/.m2/settings.xml` under a `<server>` with
`<id>central</id>`, and a GPG key published to a public keyserver:

```bash
mvn versions:set -DnewVersion=X.Y.Z
mvn versions:commit
mvn -Prelease clean deploy
```
