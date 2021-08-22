# Logback layout for Google Structured logging

## Why?

- Google indexes logs - easies and faster to filter logs.
  See [feedbackUsing the Logs Explorer](https://cloud.google.com/logging/docs/view/logs-viewer-interface)
- Aggregating logs by labels
- Log metrics
- Proper logging levels in the UI

See [structured logs](https://cloud.google.com/logging/docs/structured-logging).

## Getting started

**TODO**

## Configuration example:

```xml

<configuration>
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="ch.qos.logback.core.encoder.LayoutWrappingEncoder">
            <layout class="com.github.aaabramov.glogging.GoogleLayout">
                <!-- You have a choice with JSON encoder to use. Or create your own via implementing JsonEncoder interface -->
                <json>com.github.aaabramov.glogging.JacksonEncoder</json>
                <!-- <json>com.github.aaabramov.glogging.GsonEncoder</json> -->
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
{"timestamp":{"seconds":1629642099,"nanos":659000000},"severity":"DEBUG","message":"debug","labels":{"com.github.aaabramov/name":"Andrii","com.github.aaabramov/loggerName":"com.github.aaabramov.glogging.App"}}
{"timestamp":{"seconds":1629642099,"nanos":659000000},"severity":"INFO","message":"info","labels":{"com.github.aaabramov/name":"Andrii","com.github.aaabramov/loggerName":"com.github.aaabramov.glogging.App"}}
{"timestamp":{"seconds":1629642099,"nanos":659000000},"severity":"WARN","message":"warn","labels":{"com.github.aaabramov/name":"Andrii","com.github.aaabramov/loggerName":"com.github.aaabramov.glogging.App"}}
{"timestamp":{"seconds":1629642099,"nanos":661000000},"severity":"ERROR","message":"error java.lang.RuntimeException: BOOM\n\tat com.github.aaabramov.glogging.App.main(App.java:22)","labels":{"com.github.aaabramov/name":"Andrii","com.github.aaabramov/loggerName":"com.github.aaabramov.glogging.App"}}
{"timestamp":{"seconds":1629642102,"nanos":661000000},"severity":"DEBUG","message":"debug","labels":{"com.github.aaabramov/name":"Andrii","com.github.aaabramov/loggerName":"com.github.aaabramov.glogging.App"}}
{"timestamp":{"seconds":1629642102,"nanos":661000000},"severity":"INFO","message":"info","labels":{"com.github.aaabramov/name":"Andrii","com.github.aaabramov/loggerName":"com.github.aaabramov.glogging.App"}}
{"timestamp":{"seconds":1629642102,"nanos":661000000},"severity":"WARN","message":"warn","labels":{"com.github.aaabramov/name":"Andrii","com.github.aaabramov/loggerName":"com.github.aaabramov.glogging.App"}}
{"timestamp":{"seconds":1629642102,"nanos":661000000},"severity":"ERROR","message":"error","labels":{"com.github.aaabramov/name":"Andrii","com.github.aaabramov/loggerName":"com.github.aaabramov.glogging.App"}}
```