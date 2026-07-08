# Project guidelines — IntelliJ Project Identifier plugin

Instructions for Claude (and humans) working on this repository. Project-specific; the user's global
`~/.claude/CLAUDE.md` still applies on top of this.

## What this project is

A JetBrains IDE plugin (Kotlin, Gradle, IntelliJ Platform Gradle Plugin 2.x) that draws a subtle, low-opacity
**text watermark** on the editor/empty-frame background, per project, so multiple open windows are easy to tell
apart during task switching. Product behavior is specified in [`docs/specs/`](../docs/specs/); technical
decisions are recorded in [`docs/adrs/`](../docs/adrs/).

## ⚠️ Docs & code move together (read this first)

This repo keeps **living documentation** in [`docs/`](../docs/):

- [`docs/specs/`](../docs/specs/) — **product / behavioral** decisions (*what* the plugin does, from the user's
  point of view).
- [`docs/adrs/`](../docs/adrs/) — **technical** decisions (*how* it's built and *why that way*).

**The hard rule: when you change one, update the other in the same change.**

- If you change **code** that alters user-facing behavior → update the relevant **spec** (and any ADR whose
  "Reflected in code" section points at the files you touched).
- If you change a **spec or ADR** → change the **code** so it matches. A doc that disagrees with the code is a
  bug, not just stale text.
- Adding a feature → add/extend a spec (behavior) **and** an ADR (technical approach), and cross-link them.
- Each doc has a **Reflected in code** section listing the source files/tests that implement it. Keep those
  links correct when files move, and use them to find what else must change.
- Small evolution of an ADR → amend it with a dated note. A reversal → write a new ADR that supersedes the old
  one (set its Status). Same idea for specs.
- Update the doc's **Last updated** date and, where relevant, its **Change history**.

See [`docs/README.md`](../docs/README.md) for the spec-vs-ADR distinction and the full index. New documents use
the [spec template](../docs/specs/spec-template.md) / [ADR template](../docs/adrs/adr-template.md) and
kebab-case names (`spec-NNNN-...md`, `adr-NNNN-...md`).

## Architecture — hexagonal (see [ADR-0002](../docs/adrs/adr-0002-hexagonal-architecture.md))

Root package: `com.github.lucsartes.intellijprojectidentifierplugin`.

- **`core/`** — pure Kotlin domain and use cases. **No `com.intellij.*` or `git4idea.*` imports.** Everything
  here must be unit-testable without a running IDE (identifier derivation, template resolution, image
  rendering, `.git/HEAD` parsing, branch-change detection, on-disk storage, settings models).
- **`ports/`** — interfaces describing what the core needs from the outside world. **JDK and core domain types
  only** — no IDE/VCS SDK types may leak through a port signature. A port must abstract a *boundary to the
  outside world* (persistence, the background image, the branch source); pure algorithms with no external
  dependency stay plain `core` classes (e.g. `IdentifierGenerator`, `ImageRenderer`, `FontSupport`) constructed
  directly, not ports or registered services.
- **`adapters/intellij/`** — the only place IntelliJ SDK / VCS types are allowed. Implements the ports
  (persistent-state services, `Configurable` UIs, the background-image adapter, the branch providers, the
  watermark pipeline). Unstable/internal IDE APIs (e.g. the background-image properties) must be isolated here
  and **fail gracefully** — never let a watermark problem crash the IDE.

Boundaries to respect when editing:
- Don't reference IntelliJ/VCS types from `core` or `ports`.
- Keep new capabilities modeled as a **port** + an **adapter** implementation, wired in
  `src/main/resources/META-INF/plugin.xml` (the composition root).
- Adapters translate between IDE/runtime types and domain types.

## Build, test, verify

- Toolchain: JVM 21 (`org.gradle.java.home` is set globally to `/home/lfaviere/docs/apps/java-21`); Gradle 9;
  IntelliJ Platform `IC 2024.3.6` (since-build 243). Git4Idea is a bundled plugin dependency, used *optionally*
  at runtime for event-driven branch detection.
- Unit tests (fast, includes pure-core JUnit tests):
  `./gradlew compileKotlin compileTestKotlin test`
- Plugin Verifier (the maintainer cares about this passing):
  `./gradlew verifyPlugin verifyPluginProjectConfiguration`
- Run a sandbox IDE with the plugin: `./gradlew runIde`
- The build resolves plugins/deps from public repositories (Gradle Plugin Portal + Maven Central +
  JetBrains cache-redirector), so it works off the corporate VPN. Network-using Gradle commands need the Bash
  tool's `dangerouslyDisableSandbox: true`.

## Conventions

- **Kotlin, not Java.** Match the surrounding style. Prefer immutable `data class` domain models and pure
  functions in `core`.
- **User-facing strings are localized.** Never hardcode UI text — add a key to
  `src/main/resources/messages/MyBundle.properties` (English, base) **and** `MyBundle_fr.properties` (French),
  and read it via `MyBundle.message(...)`. Keep the key sets in sync (the i18n tests enforce this). See
  [ADR-0004](../docs/adrs/adr-0004-internationalization-implementation.md) /
  [SPEC-0005](../docs/specs/spec-0005-internationalization.md).
- **Settings.** Per-project settings persist in the workspace file; global settings in `projectIdentifier.xml`.
  A settings change must publish on its message-bus `Topic` so the watermark refreshes automatically. See
  [ADR-0003](../docs/adrs/adr-0003-settings-implementation.md).
- **Concurrency.** The watermark pipeline is serialized on a single dedicated thread with a generation counter
  so only the latest run applies; don't reintroduce overlapping renders. See
  [ADR-0005](../docs/adrs/adr-0005-branch-placeholder-implementation.md).
- **CHANGELOG.** User-facing changes go in `CHANGELOG.md` under `[Unreleased]` (Keep a Changelog format); the
  build injects the latest section into the plugin's change-notes. `README.md`'s
  `<!-- Plugin description -->` block becomes the marketplace description — keep it accurate.

## Testing

- Core logic is covered by fast, IDE-independent unit tests under `src/test/kotlin/.../core/**` and
  `.../i18n/**`. Add/adjust these when you touch core behavior.
- Provide fakes for ports rather than booting an IDE where possible;
  `.../adapters/intellij/ServiceWiringIntegrationTest.kt` covers service wiring.
- When adding user-facing behavior, add a core test for the logic **and** update the relevant spec/ADR.
