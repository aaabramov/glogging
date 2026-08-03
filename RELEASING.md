# Releasing

Maintainer documentation for publishing glogging to
[Maven Central](https://central.sonatype.com). Consumers of the library do not
need any of this — see the [README](README.md) instead.

Releases are **tag-driven**: pushing a `vX.Y.Z` tag builds, signs and publishes
the artifacts, then creates a GitHub Release. No local Maven, GPG key or
Sonatype token is required.

---

## Cutting a release

First, move the pending entries in [CHANGELOG.md](CHANGELOG.md) from `## Unreleased`
into a section for the version you are about to tag, and merge that to `master`. The
release notes on the GitHub Release are auto-generated from commit subjects, so the
changelog is the only place a behaviour change gets explained to the people it
affects — 0.1.2 changed the emitted `severity` values, which no PR title conveys.

If the release **breaks anything**, add a section to
[docs/UPGRADING.md](docs/UPGRADING.md) as well, and link it from the changelog entry.
Keep it ordered by how likely each change is to go unnoticed: a changed JSON field name
silently breaks log-based metrics and saved queries with no compile error and no failed
deploy, so it deserves to come before anything the compiler will catch for the reader.

Then:

```bash
git checkout master && git pull
git tag v0.1.2
git push origin v0.1.2
```

That's the whole procedure. The
[`Release` workflow](.github/workflows/release.yml) takes it from there and
finishes in about a minute; artifacts appear on
[repo1.maven.org](https://repo1.maven.org/maven2/io/github/aaabramov/) roughly
10–30 minutes later.

The tag name minus the leading `v` becomes the released version — the tag is the
single source of truth, and CI rewrites the poms to match at build time.

> **The version in `pom.xml` on `master` is deliberately never updated.** It
> stays at a `-SNAPSHOT` value permanently. This is a consequence of taking the
> version from the tag, and it means `git show v0.1.1:pom.xml` reports a
> *different* version than the one actually released under that tag. Only the
> published artifact — and the README, which CI rewrites — carry the real
> version.

### What the workflow does

1. Resolves the version from the tag, rejecting `-SNAPSHOT` and empty values
   up front — Central would reject them too, but only after a full build.
2. Builds on **JDK 11**. Artifacts remain Java 8 bytecode
   (`maven.compiler.target=1.8`); JDK 11 is used because the release plugins
   need a newer JDK than 8 to run cleanly.
3. Runs the `release` profile: attaches sources and javadoc, GPG-signs every
   artifact, uploads the bundle to the Central Portal.
4. Creates a GitHub Release with auto-generated notes.
5. Rewrites the dependency snippets in the README to the released version and
   commits that to `master` as `github-actions[bot]`.

Steps 4 and 5 are deliberately **last** and gated on the tag ref, so a failed
upload never leaves a GitHub Release — or a README — advertising a version that
isn't on Central. Step 5 is last of all because it is the least important: if it
fails, the release is still intact and the README can be fixed by hand.

> **The README substitution is scoped to the `## Getting started` section.** A
> repository-wide replacement would also rewrite the `git tag vX.Y.Z` example in
> this document's [Cutting a release](#cutting-a-release) section, which is
> illustrative and must not track the current version. If you add a new snippet
> carrying the version, put it in that section or the bump will skip it.

---

## Releases are permanent

A version published to Maven Central **can never be deleted or overwritten**.
There is no "unpublish". A mistake is fixed only by releasing a new version.

For a routine release the default configuration publishes straight through. To
**rehearse** a risky one — a new signing key, a large dependency jump — set
these in the `release` profile of the root `pom.xml`:

```xml
<autoPublish>false</autoPublish>
<waitUntil>validated</waitUntil>
```

The bundle is then uploaded, signed and validated, but parked in the
[Portal](https://central.sonatype.com/publishing/deployments) awaiting a manual
**Publish** click — and it can be **dropped** instead. Nothing becomes permanent
until that click. Remember to set both values back afterwards.

---

## One-time setup

### Repository secrets

Configured under **Settings → Secrets and variables → Actions**:

| Secret | Value |
| --- | --- |
| `CENTRAL_TOKEN_USERNAME` | Central Portal user token username ([central.sonatype.com/usertoken](https://central.sonatype.com/usertoken)) |
| `CENTRAL_TOKEN_PASSWORD` | Central Portal user token password |
| `GPG_PRIVATE_KEY` | ASCII-armored private signing key |
| `GPG_PASSPHRASE` | Passphrase for that key |

`actions/setup-java` generates `~/.m2/settings.xml` from the first two under the
server id `central`, and imports the signing key from the last two.

### Signing key

Maven Central requires every artifact to be GPG-signed, and verifies the
signature against a public keyserver. Any key works — it does not have to be
the same key used for previous releases.

```bash
gpg --full-generate-key                     # (1) RSA and RSA, 4096 bits, no expiry
gpg --list-secret-keys --keyid-format=long  # note the fingerprint

# Publish the PUBLIC half. Central cannot verify the signature without this.
gpg --keyserver keyserver.ubuntu.com --send-keys <FINGERPRINT>

# Store the PRIVATE half as a repository secret.
gpg --armor --export-secret-keys <FINGERPRINT> | gh secret set GPG_PRIVATE_KEY
gh secret set GPG_PASSPHRASE
```

Choose **RSA** rather than the ECC default: RSA is what Central's validation is
universally known to accept, and a signing key is not the place to discover an
incompatibility.

Back the private key up somewhere durable (a password manager) — losing it just
means generating a new one, but that is avoidable work.

---

## Gotchas

Each of these has actually bitten this project.

### `keys.openpgp.org` silently strips the User ID

That keyserver only publishes a key's User ID after the address in it has been
**verified by email**. If the key's UID uses an address that cannot receive
mail — a GitHub `noreply` address, for instance — verification can never
complete, and the server serves the key permanently stripped:

```
$ gpg --import key-from-keys-openpgp-org.asc
gpg: key <KEY_ID>: no user ID
```

The key material is there, but `gpg` refuses the import outright, which can
break signature verification downstream. **`keyserver.ubuntu.com` does not
strip UIDs** — publish there. Verify a key is actually usable before relying
on it:

```bash
curl -s "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x<FINGERPRINT>" \
  | gpg --show-keys
```

A successful `--send-keys` is *not* evidence the key is fetchable in a usable
form.

### A stale publishing plugin fails after Central accepts the bundle

`central-publishing-maven-plugin` deserializes the Portal's API responses
strictly. When Sonatype adds a field the pinned version does not know about,
the build dies like this:

```
UnrecognizedPropertyException: Unrecognized field "warnings"
  (class ...DeploymentApiResponse), not marked as ignorable
```

This broke the 0.1.1 release on plugin 0.7.0; 0.11.0 added the field.

The dangerous part is the ordering: the failure happens **after** Central has
already accepted the upload. With `autoPublish=true` the build can go red for a
release that published anyway. If a release fails inexplicably, check the plugin
version first, and check the Portal for an orphaned deployment before retrying —
drop it so it can't be confused with the retry.

Dependabot now watches this plugin (see
[`.github/dependabot.yml`](.github/dependabot.yml)), which is largely why it is
configured at all.

### The Java 8 baseline is easy to break by accident

The library targets Java 8, so some dependencies are pinned deliberately and are
excluded from Dependabot:

- **JUnit** — 6.x requires Java 17, while the whole 5.x line keeps a Java 8
  baseline. Major updates ignored; minors within 5.x are fine and the JDK 8 job
  in the build matrix proves it (5.14.4 passes).
- **logback** — 1.3.x targets JDK 8, while 1.4.x and 1.5.x target JDK 11. The
  break therefore arrives as a **minor** version bump, which is exactly the kind
  of update that gets merged without a second look. Major *and minor* updates
  ignored.

If you deliberately drop Java 8 support, remove those `ignore` rules and raise
`maven.compiler.source`/`target` together.

---

## Releasing from a workstation

Not the recommended path — prefer the workflow, which needs no local
credentials. Requires a Central Portal user token in `~/.m2/settings.xml` under
a `<server>` with `<id>central</id>`, and a published signing key:

```bash
mvn versions:set -DnewVersion=X.Y.Z
mvn versions:commit
mvn -Prelease clean deploy
```

---

## Verifying a release

Confirm the artifacts landed, and that the signature validates the way a
consumer's build would:

```bash
V=0.1.2
B=https://repo1.maven.org/maven2/io/github/aaabramov/glogging-core/$V
curl -sO $B/glogging-core-$V.jar
curl -sO $B/glogging-core-$V.jar.asc
gpg --verify glogging-core-$V.jar.asc glogging-core-$V.jar
```

`Good signature` means the published artifact, the published key and the
keyserver all agree.
