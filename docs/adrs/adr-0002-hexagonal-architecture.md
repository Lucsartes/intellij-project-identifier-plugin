# ADR-0002: Hexagonal architecture

> Technical decision. It underpins essentially every spec by keeping product logic testable and IDE-independent.
> Living document — keep in sync with the code (see [`docs/README.md`](../README.md)).

* **Status**: Accepted
* **Last updated**: 2026-07-08 (originally decided 2025-09-09)
* **Authors**: Project maintainers

## 1. Context

The plugin's core behavior (identifier derivation, template resolution, image rendering) must be reliable and
testable, while the parts that touch the IntelliJ Platform SDK — especially the internal background-image API
from [ADR-0001](adr-0001-dynamic-image-generation.md) — are the least stable and hardest to test. We need a
structure that keeps the volatile IDE surface small and the valuable logic pure.

## 2. Decision drivers

* **Maintainability** — decouple domain logic from IDE APIs so each can change independently.
* **Testability** — exercise core logic in fast JVM unit tests, with no running IDE.
* **API-stability containment** — confine unstable IDE/VCS APIs to a thin, swappable layer.
* **Practicality** — stay compatible with the official IntelliJ Platform Plugin Template's build and layout.

## 3. Considered options

* **Layered hexagonal (core / ports / adapters).** Pure domain in the center, interfaces (ports) describing
  what the domain needs, adapters implementing those ports against the SDK. Pros: clear boundaries, testable
  core, isolated instability. Cons: some upfront boilerplate and indirection.
* **Flat plugin code (SDK calls throughout).** Pros: least boilerplate initially. Cons: business logic tangled
  with IDE types, hard to unit-test, and every IDE-API break ripples widely.

## 4. Decision

Adopt a **hexagonal architecture** with three packages under
`com.github.lucsartes.intellijprojectidentifierplugin`:

* **`core`** — pure Kotlin domain and use cases; **no IntelliJ (or VCS) imports**. Contains the settings models
  (`ProjectSettings`, `ApplicationSettings`), identifier derivation (`IdentifierGenerator`), image rendering
  (`ImageRenderer`), the font-resolution policy (`FontSupport`), placeholder resolution (`TemplateResolver`),
  branch-change detection (`BranchChangeDetector`), `.git/HEAD` parsing (`GitHeadParser`), and on-disk storage
  (`WatermarkStore`).
* **`ports`** — interfaces describing what the core needs from the outside world: `ProjectSettingsPort`,
  `ApplicationSettingsPort`, `BackgroundImagePort`, `BranchProvider`. JDK and core domain types only (no
  IntelliJ/VCS SDK types) — settings ports naturally reference the core models `ProjectSettings` /
  `ApplicationSettings`.
* **`adapters/intellij`** — implementations that talk to the IntelliJ SDK (services, configurables, the
  background-image adapter, the branch providers, the pipeline). This is where all SDK/VCS types live.

> **Refinement (2026-07-08).** A port must abstract a *boundary to the outside world* (settings persistence,
> the background image, the branch source). Identifier derivation and image rendering are pure algorithms with
> no external dependency, so they are plain `core` classes (`IdentifierGenerator`, `ImageRenderer`) constructed
> directly by the pipeline — not ports, not registered services. They were previously modeled as
> `IdentifierService` / `ImageService` ports; that indirection abstracted nothing and was removed.

Inflow adapters are declared in `plugin.xml` (the post-startup activity, the configurables, and the service
registrations), which is the configuration entry point that drives the core.

Note the acronym-derivation logic lives in the core (see
[SPEC-0002](../specs/spec-0002-identifier-derivation.md)), which is exactly the kind of behavior this
architecture keeps pure and unit-testable.

## 5. Consequences

* **Positive**: a robust, testable, maintainable codebase; the unstable IDE surface is a handful of adapters;
  the branch feature could add a whole detection strategy without touching the core (see
  [ADR-0005](adr-0005-branch-placeholder-implementation.md)).
* **Negative**: slightly more indirection and boilerplate (ports + adapter wiring). Accepted for the long-term
  isolation of unstable APIs.
* **Neutral**: `plugin.xml` acts as the composition root, wiring port interfaces to their implementations.

## 6. Reflected in code

- `src/main/kotlin/.../core/**` — pure domain; must contain no `com.intellij` / `git4idea` imports.
- `src/main/kotlin/.../ports/**` — the four port interfaces (JDK and core domain types only; no IntelliJ/VCS
  SDK types).
- `src/main/kotlin/.../adapters/intellij/**` — all SDK-facing implementations.
- `src/main/resources/META-INF/plugin.xml` — composition root binding interfaces to implementations
  (`projectService` / `applicationService` `serviceInterface` → `serviceImplementation`); it also registers the
  standard JetBrains Marketplace error-report submitter (`errorHandler`), so an uncaught plugin exception offers
  the IDE's "report to JetBrains" dialog rather than being silently lost — separate from the watermark
  pipeline's own graceful failure handling.
- Tests: everything under `src/test/kotlin/.../core/**` runs without an IDE;
  `.../adapters/intellij/ServiceWiringIntegrationTest.kt` checks the wiring.

## 7. Related documents

- **Serves**: directly [SPEC-0001 — Project watermark](../specs/spec-0001-project-watermark.md) and
  [SPEC-0002 — Identifier derivation](../specs/spec-0002-identifier-derivation.md); as a cross-cutting
  architectural foundation it also underpins every other spec.
- **Related ADRs**: [ADR-0001 — Dynamic image generation](adr-0001-dynamic-image-generation.md) (the primary
  reason to isolate instability), [ADR-0003](adr-0003-settings-implementation.md),
  [ADR-0005](adr-0005-branch-placeholder-implementation.md).
