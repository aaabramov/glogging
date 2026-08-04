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

## The Java 8 baseline, and how the build enforces it

The library targets Java 8, and the build compiles against the Java 8 **class library** —
not merely down to Java 8 bytecode. So if you reach for an API that does not exist on 8,
compilation fails immediately:

```
Label.java:[28,31] cannot find symbol
  symbol: method isBlank()
```

If you see that for a method your IDE happily autocompletes, you have used something newer
than Java 8. That is the guard working, not a broken build.

This is worth knowing because it is *not* what `-source`/`-target` alone do. Those set the
bytecode level but still compile against the **running** JDK's class library, so the same
call would compile cleanly, land in a class file stamped Java 8, and only fail with
`NoSuchMethodError` on someone else's Java 8 JVM. `maven.compiler.release` closes that, and
`pom.xml` applies it through a `jdk9plus` profile — the option needs JDK 9+, and on JDK 8
itself the class library is already the right one.

Two dependencies are pinned for the same baseline and excluded from Dependabot: JUnit stays
on 5.x (6.x needs Java 17) and logback on 1.3.x (1.4+ needs Java 11). Please don't bump
either in a PR.

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
