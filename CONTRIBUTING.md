# Contributing

Thanks for looking. glogging is a **small** logback layout that writes GCP-shaped JSON to
stdout, and staying small is a design goal rather than an accident — the README's
[positioning table](README.md#is-this-the-right-library-for-you) exists so that people who
need more are pointed somewhere better. A feature PR may well be declined on scope even if
the code is good, so for anything beyond a bug fix please **open an issue first** and let's
agree it belongs here before you write it.

Maintainer release documentation lives in [RELEASING.md](RELEASING.md); you do not need any
of it to contribute.

## Building and testing

```bash
mvn -B verify
```

That runs the whole reactor: `core`, `gson`, `jackson`. Any JDK from 8 upwards builds it —
CI runs the matrix **8, 11, 17, 21** and all four must pass.

## The Java 8 baseline is easy to break without noticing

The library targets Java 8 (`maven.compiler.source`/`target` in `pom.xml`). Those flags set
the *bytecode* level but still compile against the **current JDK's class library**, so
nothing stops you calling a method that does not exist on Java 8. It compiles, it ships, and
it fails at runtime for anyone actually on 8.

The JDK 8 leg of the CI matrix is what catches this, so **don't ignore a failure that only
appears there** — it is the one job testing the thing the build cannot check locally.

Two dependencies are pinned for the same reason and excluded from Dependabot: JUnit stays on
5.x (6.x needs Java 17) and logback on 1.3.x (1.4+ needs Java 11). Please don't bump either
in a PR.

## Configuration must be tested through real logback XML

This is the most important convention here, and the least obvious.

logback's configurator (Joran) wires XML elements to your class **by reflection** — a
`<label>` element finds `addLabel(Label)` by naming convention, with nothing checked at
compile time. So a test that calls `layout.addLabel(...)` from Java **passes even when the
XML could never have wired the element at all**, which is precisely the bug worth catching.

Configuration changes therefore need an integration test that drives real XML through
`JoranConfigurator`. Use the existing harness:

- [`LogbackFixture`](core/src/test/java/io/github/aaabramov/glogging/LogbackFixture.java) —
  builds a throwaway `LoggerContext` from an XML string.
- [`RecordingEncoder`](core/src/test/java/io/github/aaabramov/glogging/RecordingEncoder.java)
  — captures the event map without a JSON library.
- `StaticLabelsConfigTest` and `StaticLabelsValidationTest` are the pattern to copy; the
  encoder modules additionally parse bytes captured from real stdout.

One trap the fixture already handles for you: **a hand-built `LoggerContext` has no MDC
adapter**, and every append then throws `NullPointerException` inside logback, which
swallows it into the `StatusManager` — so a naive test records nothing and tells you
nothing about why. The fixture sets one. Write MDC through `ctx.getMDCAdapter()`, not the
static `org.slf4j.MDC`, which belongs to a different context and is invisible here.

Also: **a layout must never throw.** Bad configuration is reported via `addError` /
`addWarn` on the `StatusManager` and then skipped, so the application keeps logging. Test
that behaviour rather than an exception.

## Dependencies

`core` has no runtime dependencies. logback is `provided`, because glogging is an extension
*to* logback and the application already has it — a `compile` scope would push our version
onto every consumer. Gson and Jackson are isolated in their own modules behind the
`JsonEncoder` SPI.

Please don't add a dependency to `core`. If something needs one, it belongs in a separate
optional module, loaded the way the encoders are.

## Commits and pull requests

- **Branch and open a PR.** Nothing is pushed straight to `master`.
- **Commit subjects** use a conventional prefix — `feat:`, `fix:`, `docs:`, `test:`,
  `chore:`, `ci:` — with `!` for a breaking change, e.g. `fix!: …`. PRs are squash-merged,
  so the PR title becomes the commit subject on `master`; make it a sentence worth reading
  in `git log`.
- **No AI-tool attribution in commit messages.** No `Claude-Session:` trailer, no
  `Co-Authored-By: Claude`, no "generated with" line. Git history here records authorship,
  not which editor helped. A CI check enforces this on every PR; to catch it locally before
  pushing, enable the repo's hook once per clone:

  ```bash
  git config core.hooksPath .githooks
  ```

  That is optional — the CI check is the enforcement — but it fails in a second instead of
  after a push. If your assistant added the trailer, the setting that stops it at the source
  is `attribution.sessionUrl: false` (a *separate* field from `attribution.commit` and
  `attribution.pr`).
- **User-visible changes need a `CHANGELOG.md` entry** under `## Unreleased`. The GitHub
  release notes are generated from commit subjects, so the changelog is the only place a
  behaviour change gets explained to the people it affects.

## Licence

Contributions are accepted under the [Apache License 2.0](LICENSE), the same licence the
project ships under.
