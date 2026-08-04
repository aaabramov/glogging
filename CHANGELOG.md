# Changelog

Notable changes to glogging. Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/);
this project uses [Semantic Versioning](https://semver.org/spec/v2.0.0.html), with the
pre-1.0 caveat that a breaking change bumps the **minor** version.

All three artifacts — `glogging-core`, `glogging-gson`, `glogging-jackson` — share a
single version, so one entry covers them all.

## Unreleased

## 0.3.0 — 2026-08-04

### Added

- **Static labels from configuration.** Repeated `<label>` elements on the layout attach
  labels to every log line, so deployment constants such as service name, version and
  region no longer have to be pushed into the MDC by hand on every thread that logs:

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

  Values go through logback's `${...}` substitution before glogging sees them, so
  environment-derived labels — `${K_REVISION}` on Cloud Run, `${HOSTNAME}` on GKE — work
  with no extra support. `<prefix>` applies to them exactly as it does to MDC keys, and
  an MDC entry overrides a static label on a key collision, since per-event data is more
  specific than a deployment default.

  A `<label>` missing its key or value is reported on logback's status output and
  skipped; it never stops the application logging. Note that logback trims element text
  and collapses `<value></value>` to nothing, so an empty value is rejected in the same
  way a missing one is. ([#26])

**Nothing changes for existing configurations.** A layout with no `<label>` elements
emits byte-identical output to 0.2.0, and no encoder was modified — static labels are
additional entries in the `logging.googleapis.com/labels` object that already exists.

## 0.2.0 — 2026-08-04

**Breaking.** Changes the emitted JSON, the `JsonEncoder` interface, and what glogging
puts on your classpath. See [docs/UPGRADING.md](docs/UPGRADING.md) for the
whole migration in one place; each entry below also carries its own action-required note.

### Fixed

- **Labels now reach `LogEntry.labels`.** They are emitted under the special
  `logging.googleapis.com/labels` key instead of a plain `labels` object. A bare `labels`
  object is not a special field, so Cloud Logging never promoted it — labels stayed inside
  `jsonPayload`, unusable for label-based filtering and log-based metrics. This was the
  library's headline feature and it did not work.

  Verified against a real GKE cluster before and after, not inferred from documentation:

  | Emitted | `LogEntry.labels` | Payload |
  |---|---|---|
  | `"labels":{…}` (≤ 0.1.2) | ours **absent** | `jsonPayload.labels` |
  | `"logging.googleapis.com/labels":{…}` (0.2.0) | ours **present** | `textPayload` |

  > **Action required.** Logs Explorer queries, log-based metrics and alerts written
  > against `jsonPayload.labels.*` must move to `labels.*`, e.g.
  > `labels."com.yourcompany/userId"="42"`. ([#21])

- `GcpTimestamp` no longer produces a negative `nanos` for pre-1970 instants. The two
  fields model a protobuf `Timestamp`, which requires `0 <= nanos <= 999999999`;
  truncating division produced e.g. `{seconds: -1, nanos: -500000000}` for `-1500ms`.
  Unreachable from a logging event on a sane clock, so this is correctness rather than a
  user-visible fix. ([#22])

### Changed

- **Entries no longer have a `jsonPayload`; the message is in `textPayload`.** A
  consequence of the fix above: once every emitted field is one Cloud Logging recognises,
  it collapses the payload. Queries on `jsonPayload.message` must move to `textPayload`.
  Everything `<pattern>` puts in the message, `%xException` included, is preserved there,
  so Error Reporting still picks up stack traces. ([#21])
- **`JsonEncoder.toJson` now takes `Map<String, Object>`** rather than the
  package-private `GcpLoggingEvent`. Required by the fix — `logging.googleapis.com/labels`
  is not expressible as a Java field name, and `core` carries no Gson or Jackson
  annotations to rename one. A welcome side effect: the interface is now genuinely
  implementable outside the library's own package, which the README had claimed since
  0.0.1 without it being true. ([#21])
- An empty label set is omitted entirely rather than emitted as `{}`. ([#21])
- **logback is now `provided`, not `compile`.** glogging is an extension to logback, so it
  no longer ships one: `glogging-core` used to export logback 1.3.x onto every consumer,
  where it competed with whatever logback the application actually ran. Nearest-wins
  usually spared an app on 1.5.x/1.6.x, but that is luck, not design.

  ```
  before:  glogging-gson -> glogging-core -> logback-core   1.3.16 (compile)
                                          -> logback-classic 1.3.16 (compile)
                                             -> slf4j-api     2.0.7 (compile)
  after:   glogging-gson -> glogging-core          (no logback, no slf4j)
  ```

  > **Action required if glogging was your only source of logback.** Declare
  > `ch.qos.logback:logback-classic` (1.3 or newer) yourself. Applications that already
  > get logback from elsewhere — `spring-boot-starter` and friends — need no change, and
  > will stop being at risk of a silent downgrade to 1.3.x. ([#24])

### Documentation

- README now states **when to use glogging and when not to**, comparing it against Log4j2's
  built-in `GcpLayout.json` template, the official `google-cloud-logging-logback` appender,
  and `logstash-logback-encoder`. If you are on Log4j2, use its built-in template — glogging
  is logback-only. ([#23])
- Added a [Labels](README.md#labels) section explaining how MDC entries become queryable
  `LogEntry.labels`, a [Custom JSON encoder](README.md#custom-json-encoder) example — now
  that the SPI is actually implementable — and a note that a JSON-valued message stays a
  string, searchable with `textPayload:"..."` but not structurally queryable. ([#21])

### Internal

- Fixed the release workflow's README bump, which failed on its first run (v0.1.2) by
  calling `git commit` with nothing staged. The 0.1.2 release itself was unaffected —
  the step runs last precisely so that a failure there cannot damage a release — and
  the README was corrected by hand. ([#20])

## 0.1.2 — 2026-08-03

### Fixed

- **Severity now uses the canonical Google Cloud Logging
  [`LogSeverity`](https://cloud.google.com/logging/docs/reference/v2/rest/v2/LogEntry#logseverity)
  names.** The layout previously passed logback's level string straight through, emitting
  `"severity":"WARN"` and `"severity":"TRACE"` — neither of which is a `LogSeverity` value.
  The collector tries to match common severity strings, so `WARN` often worked anyway, but
  that leniency is undocumented, and an unmatched value lands as `DEFAULT`, which sorts
  below `DEBUG` and is easy to miss in the Logs Explorer. ([#18])

  | logback | before | now |
  |---|---|---|
  | `WARN` | `WARN` | `WARNING` |
  | `TRACE` | `TRACE` | `DEBUG` |

  `ERROR`, `INFO` and `DEBUG` were already correct and are unchanged.

  > **Action required if you match the raw string.** A log-based metric, saved Logs
  > Explorer query, or alert filter written against `WARN` must be updated to `WARNING`.
  > If you filter on the parsed `severity` field instead, there is most likely nothing
  > to do — the collector was probably already normalising the value for you.

### Changed

- **Jars now declare `Automatic-Module-Name`** — `io.github.aaabramov.glogging.core`,
  `.gson`, `.jackson`. JPMS consumers previously got a module name the JDK derived from
  the jar filename, which was unstable across releases. ([#17])
- **Builds are reproducible.** `project.build.outputTimestamp` is pinned, so a given
  source tree produces byte-identical artifacts. ([#17])

### Internal

- `maven-jar-plugin` pinned to 3.5.1 rather than inherited from the super-POM. ([#17])
- Release workflow actions bumped to v5 and `autoPublish` restored, so a tag push
  publishes without a manual step in the Portal. ([#9])
- The `Build` workflow runs with `permissions: contents: read`. ([#17])
- The Maven Central badge moved to shields.io; the previous
  `maven-badges.herokuapp.com` endpoint had gone dead. ([#17])
- Dependabot now covers GitHub Actions and Maven, with the release toolchain grouped and
  Java-8-breaking majors pinned. ([#10], [#13], [#11], [#12])
- Release process documented in [RELEASING.md](RELEASING.md). ([#14])
- The release workflow points the README's dependency snippets at the released
  version. ([#15], [#16])

## 0.1.1 — 2026-08-03

### Fixed

- Publishing repaired end to end: migrated to the Sonatype Central Portal after the OSSRH
  / Nexus staging flow was retired, and releases are now built, signed and uploaded by
  GitHub Actions off a version tag. ([#6], [#7], [#8])

### Changed

- Dependencies updated and the first unit tests added. ([#6])

## 0.1.0 — 2023-02-22

- Dependency updates and README corrections. ([#5])

## 0.0.1 — 2021-08-22

- First release: `GoogleLayout` for logback, with interchangeable Gson and Jackson JSON
  encoders.

[#5]: https://github.com/aaabramov/glogging/pull/5
[#6]: https://github.com/aaabramov/glogging/pull/6
[#7]: https://github.com/aaabramov/glogging/pull/7
[#8]: https://github.com/aaabramov/glogging/pull/8
[#9]: https://github.com/aaabramov/glogging/pull/9
[#10]: https://github.com/aaabramov/glogging/pull/10
[#11]: https://github.com/aaabramov/glogging/pull/11
[#12]: https://github.com/aaabramov/glogging/pull/12
[#13]: https://github.com/aaabramov/glogging/pull/13
[#14]: https://github.com/aaabramov/glogging/pull/14
[#15]: https://github.com/aaabramov/glogging/pull/15
[#16]: https://github.com/aaabramov/glogging/pull/16
[#17]: https://github.com/aaabramov/glogging/pull/17
[#18]: https://github.com/aaabramov/glogging/pull/18
[#20]: https://github.com/aaabramov/glogging/pull/20
[#21]: https://github.com/aaabramov/glogging/pull/21
[#22]: https://github.com/aaabramov/glogging/pull/22
[#23]: https://github.com/aaabramov/glogging/pull/23
[#24]: https://github.com/aaabramov/glogging/pull/24
[#26]: https://github.com/aaabramov/glogging/pull/26
