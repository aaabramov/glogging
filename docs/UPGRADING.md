# Upgrading

Migration notes for releases that need them, newest first. Releases not listed here are
drop-in — see [CHANGELOG.md](../CHANGELOG.md) for the full history.

## 0.1.x → 0.2.0

0.2.0 fixes labels, which had never actually worked, and the fix changes the emitted JSON.
Four things may need attention. The first two affect your **queries**, not your code, so
they will not show up as a compile error — check them before deploying.

### 1. Label filters move out of the payload

Labels are now emitted under `logging.googleapis.com/labels`, so Cloud Logging promotes
them to `LogEntry.labels` instead of leaving them in `jsonPayload`:

```diff
- jsonPayload.labels."com.yourcompany/userId"="42"
+ labels."com.yourcompany/userId"="42"
```

### 2. The message moves to `textPayload`

Every field glogging emits is now one Cloud Logging recognises, so it collapses the
payload and there is no `jsonPayload` at all:

```diff
- jsonPayload.message:"connection refused"
+ textPayload:"connection refused"
```

Update any **log-based metrics, saved queries, alerting policies and dashboards** built on
the old paths. Stack traces are unaffected: `%xException` output still travels in the
message, so Error Reporting keeps working.

### 3. Declare logback yourself if glogging was your only source of it

logback is now `provided`, so glogging no longer puts its own 1.3.x on your classpath —
where it competed with whatever logback you actually run:

```xml
<dependency>
    <groupId>ch.qos.logback</groupId>
    <artifactId>logback-classic</artifactId>
    <version>1.5.x</version> <!-- or 1.3.x on Java 8 -->
</dependency>
```

Most applications already get logback via `spring-boot-starter` or similar and need no
change — those also stop being at risk of a silent downgrade to 1.3.x.

### 4. Only if you wrote your own `JsonEncoder`

The method now takes a `Map<String, Object>` instead of the (package-private, hence
unusable) event type. See
[Custom JSON encoder](../README.md#custom-json-encoder). If you use `GsonEncoder` or
`JacksonEncoder`, there is nothing to do.

### Why these changes were made

A plain top-level `labels` object is not one of Cloud Logging's
[special fields](https://cloud.google.com/logging/docs/structured-logging), so the
collector never promoted it — labels sat in `jsonPayload`, unusable for label-based
filtering or log-based metrics, which was the library's headline feature. This was
confirmed by emitting both shapes from a real GKE workload and reading the resulting
`LogEntry` back, before and after the fix.
