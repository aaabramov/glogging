# Changelog

Notable changes to glogging. Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/);
this project uses [Semantic Versioning](https://semver.org/spec/v2.0.0.html), with the
pre-1.0 caveat that a breaking change bumps the **minor** version.

All three artifacts — `glogging-core`, `glogging-gson`, `glogging-jackson` — share a
single version, so one entry covers them all.

## Unreleased

### Fixed

- `GcpTimestamp` no longer produces a negative `nanos` for pre-1970 instants. The two
  fields model a protobuf `Timestamp`, which requires `0 <= nanos <= 999999999`;
  truncating division produced e.g. `{seconds: -1, nanos: -500000000}` for `-1500ms`.
  Unreachable from a logging event on a sane clock, so this is correctness rather than a
  user-visible fix.

### Internal

- Fixed the release workflow's README bump, which failed on its first run (v0.1.2) by
  calling `git commit` with nothing staged. The 0.1.2 release itself was unaffected —
  the step runs last precisely so that a failure there cannot damage a release — and
  the README was corrected by hand.

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
