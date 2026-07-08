# SPEC-0001: Project watermark

> Product/behavioral spec. Describes *what* the plugin does for the user. The technical realization lives in
> the ADRs linked in §6. Keep this document in sync with the code (see [`docs/README.md`](../README.md)).

* **Status**: Accepted
* **Last updated**: 2026-07-08
* **Owners**: Project maintainers

## 1. Summary

Project Identifier helps a developer who keeps several IDE windows open tell them apart at a glance. When a
project is opened, the plugin draws a subtle, low-opacity **text watermark** onto the editor background (and
the empty-frame background shown when no file is open). The watermark text is derived from the project — by
default a short acronym of its name — so each window carries a distinct visual marker that is visible even in a
task switcher (`Alt+Tab`, the Windows key, Mission Control, etc.).

## 2. Context & motivation

Developers who juggle many projects (microservices, multiple checkouts of the same repo, client projects) lose
time working out *which* window is which — the title bar text is easy to miss when switching quickly. A large,
faint marker sitting behind the code is glanceable and unobtrusive: readable during a task switch, invisible
enough not to distract while coding.

A user could already do this by hand — generate an image with some text and set it as the IDE background — but
that is a fiddly, multi-step, per-project chore. The plugin's value is automating it.

## 3. Behavior

**When the watermark is (re)generated.** The plugin recomputes and reapplies the watermark:
- when a project is opened;
- when the plugin's project-level or global settings change (see [SPEC-0003](spec-0003-settings-and-scopes.md));
- when the current Git branch changes, if the identifier uses the `${branch}` placeholder (see [SPEC-0004](spec-0004-branch-placeholder.md)).

**What the user sees.**
- A transparent image containing the identifier text is applied as the background of both the editor and the
  empty frame, scoped to the current project only. Other open projects are unaffected.
- The text is rendered opaque inside the image; the *on-screen* faintness comes from the IDE's background-image
  opacity, which defaults to a low value the first time the plugin applies an image but remains fully under the
  user's control thereafter (see §4 and [SPEC-0003](spec-0003-settings-and-scopes.md)).
- The text content, font, size, and color follow the rules in [SPEC-0002](spec-0002-identifier-derivation.md)
  and the user's settings.

**Persistence & isolation.**
- The generated image is stored on disk in a per-project location. Regenerating a project's watermark cleans up
  that project's own previous image; it never deletes another project's image, even when two project names share
  a prefix (e.g. `shop` and `shop-api`).
- Because the image is a normal background image, the user is free to change or clear it via the IDE's own
  Background Image settings at any time.

**Graceful behavior.**
- If the identifier resolves to empty text (e.g. a blank project name), the plugin produces a minimal
  transparent image rather than failing.
- Any failure while generating or applying the watermark is contained: it is logged and the IDE keeps running
  normally. The plugin never blocks or crashes the IDE because of a watermark problem.

## 4. Non-goals / out of scope

- **Watermark position, opacity, scaling, and tiling are not plugin settings.** They are owned by the IDE's
  *Appearance & Behavior | Appearance | Background Image* page. The plugin only decides the *content* of the
  image; the IDE decides how it is displayed. This boundary is specified in
  [SPEC-0003 §Non-goals](spec-0003-settings-and-scopes.md) and realized in
  [ADR-0003](../adrs/adr-0003-settings-implementation.md).
- The plugin does not draw directly on the editor canvas or overlay UI components; it only produces a
  background image (the *why* is in [ADR-0001](../adrs/adr-0001-dynamic-image-generation.md)).

## 5. Reflected in code

- `src/main/kotlin/.../adapters/intellij/ProjectStartupActivity.kt` — runs on project open; wires the refresh
  triggers (startup, settings changes, branch changes).
- `src/main/kotlin/.../adapters/intellij/WatermarkPipelineService.kt` — the derive → render → persist → apply
  pipeline.
- `src/main/kotlin/.../core/ImageRenderer.kt` — renders the transparent PNG with opaque text.
- `src/main/kotlin/.../core/WatermarkStore.kt` — per-project on-disk storage and cleanup isolation.
- `src/main/kotlin/.../adapters/intellij/IntelliJBackgroundImageAdapter.kt` — applies the image to the editor
  and empty-frame backgrounds.
- Tests: `src/test/kotlin/.../core/ImageRendererTest.kt`,
  `.../core/ImageRendererEdgeCasesTest.kt`, `.../core/WatermarkStoreTest.kt`,
  `.../adapters/intellij/ServiceWiringIntegrationTest.kt`.

## 6. Related decisions

- **Realized by**: [ADR-0001 — Dynamic image generation](../adrs/adr-0001-dynamic-image-generation.md),
  [ADR-0002 — Hexagonal architecture](../adrs/adr-0002-hexagonal-architecture.md).
- **See also**: [SPEC-0002 — Identifier derivation](spec-0002-identifier-derivation.md),
  [SPEC-0003 — Settings & scopes](spec-0003-settings-and-scopes.md),
  [SPEC-0004 — Branch placeholder](spec-0004-branch-placeholder.md).

## 7. Change history

- 2025-10-09 — Watermark now also shown on the empty frame (not only the editor).
- 2025-10-01 — Initial behavior: automatic watermark derived from the project name.
