# ADR-0002: Hexagonal architecture (core / ports / adapters)

* **Status**: Accepted
* **Decided**: 2025-09-09
* **Last updated**: 2026-09-24

## 1. Context

The valuable logic of the plugin (identifier derivation, placeholder resolution, image rendering) must be
reliable and easy to test. The parts that touch the IntelliJ Platform SDK are the least stable and the hardest
to test, especially the internal background-image API from [ADR-0001](adr-0001-dynamic-image-generation.md). We
need a structure that keeps that volatile surface small and the logic pure.

## 2. Decision drivers

* **Testability** — core logic runs in fast JVM unit tests, with no IDE.
* **API-stability containment** — unstable IDE and VCS APIs stay in a thin, replaceable layer.
* **Maintainability** — domain logic and IDE integration can change independently.
* **Practicality** — stay close to the official IntelliJ Platform Plugin Template's build and layout.

## 3. Considered options

* **A — Hexagonal: core / ports / adapters.** Pure logic in the center, interfaces for what it needs from the
  outside, adapters implementing them on the SDK. Pros: clear boundaries, testable core, instability isolated.
  Cons: some extra interfaces and wiring.
* **B — Flat plugin code, with SDK calls wherever needed.** Pros: least code at first. Cons: logic tangled with
  IDE types, hard to unit-test, and every IDE-API break spreads widely.

## 4. Decision

Adopt **option A**, with three packages under `com.github.lucsartes.intellijprojectidentifierplugin`:

* **`core`** — pure Kotlin: the domain models and the building blocks of the watermark (derivation,
  placeholder resolution, rendering, `.git/HEAD` parsing, on-disk storage). **No `com.intellij` or `git4idea`
  imports.**
* **`ports`** — interfaces for the **boundaries to the outside world**: settings persistence, the IDE
  background image, and the branch source. Their signatures use only JDK and core types.
* **`adapters/intellij`** — everything that uses the SDK: persistent services, settings pages, the port
  implementations, the startup activity, and the refresh pipeline service.

Rules that follow from this:

* **A port exists only for an outside boundary.** Pure algorithms are plain `core` classes that callers create
  directly, not ports or registered services. (Derivation and rendering used to sit behind ports; that
  indirection abstracted nothing and was removed.)
* **The orchestration lives in an adapter.** The derive → render → store → apply sequence depends on IDE
  services, threading and project lifecycle, so it is an adapter-side service built from pure core pieces (see
  [ADR-0006](adr-0006-serialized-refresh-pipeline.md)). The core holds no use-case class of its own.
* **`plugin.xml` is the composition root.** It binds each port to its implementation, and optional descriptors
  can swap an implementation (see [ADR-0005](adr-0005-branch-placeholder-implementation.md)).

## 5. Consequences

* **Positive** — most logic is covered by IDE-free tests, and the unstable surface is a handful of adapters.
  The branch feature added a whole second detection strategy without touching the core.
* **Negative** — a few more interfaces and some wiring in `plugin.xml`. Accepted in exchange for isolating
  unstable APIs.

## 6. Code pointers

* `core/`, `ports/`, `adapters/intellij/` — the three layers.
* `src/main/resources/META-INF/plugin.xml` — the composition root.

## 7. Related

* **Serves**: every spec, as the structural foundation. Most directly
  [SPEC-0001](../specs/spec-0001-project-watermark.md) and
  [SPEC-0002](../specs/spec-0002-identifier-derivation.md).
* **Related ADRs**: [ADR-0001](adr-0001-dynamic-image-generation.md) (the main reason to isolate instability),
  [ADR-0003](adr-0003-settings-implementation.md), [ADR-0005](adr-0005-branch-placeholder-implementation.md),
  [ADR-0006](adr-0006-serialized-refresh-pipeline.md).

## Amendments

* **2026-07-08 — Ports only for real boundaries.** The `IdentifierService` / `ImageService` ports were removed
  in favor of plain core classes (the first rule in §4).
