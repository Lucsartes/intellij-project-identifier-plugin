# ADR-0001: Dynamic image generation for the watermark

> Technical decision. The product behavior it serves is [SPEC-0001 — Project watermark](../specs/spec-0001-project-watermark.md).
> Living document — keep in sync with the code (see [`docs/README.md`](../README.md)).

* **Status**: Accepted
* **Last updated**: 2026-07-08 (originally decided 2025-08-21)
* **Authors**: Project maintainers

## 1. Context

The plugin must place a low-opacity text marker behind the editor and empty frame, per project, visible during
task switching (see [SPEC-0001](../specs/spec-0001-project-watermark.md)). The technical question is *how* to
get such a marker onto the IDE's UI without being intrusive and without a large maintenance burden.

## 2. Decision drivers

* **Fidelity to the desired UX** — the marker must be a true, behind-the-code background element, not an
  overlay that competes with editor content.
* **Feasibility** — achievable with the public or reasonably accessible parts of the IntelliJ Platform SDK.
* **Performance** — no noticeable lag or resource drain.
* **Per-project scope** — the marker must be settable per project.
* **API-stability risk** — internal/undocumented APIs may break across IDE versions; minimize and isolate the
  exposure.

## 3. Considered options

* **Strategy 1 — Dynamic image generation.** Programmatically render a transparent PNG with the identifier
  text, store it, and set it as the editor/frame background via the IDE background-image mechanism.
  * Pros: integrates with the IDE's built-in background feature, giving a genuine background watermark that is
    visible during task switching; the whole flow is something a user can do by hand today, confirming the end
    state is valid — the plugin just automates the tedious steps.
  * Cons: relies on internal IDE background-image properties (applied via `IdeBackgroundUtil.EDITOR_PROP` /
    `FRAME_PROP`), which may change across IDE releases; requires file I/O and cleanup.
* **Strategy 2 — Direct rendering** onto the editor canvas via a custom renderer.
  * Pros: no file I/O; uses public painting APIs.
  * Cons: high complexity (deep knowledge of custom painting); risks drawing over code and interfering with the
    coding experience.
* **Strategy 3 — Alternative UI component** (status bar / tool window).
  * Pros: simple, stable, public APIs.
  * Cons: fails the core UX — not a background watermark, and not visible in a task switcher.

## 4. Decision

Adopt **Strategy 1 (dynamic image generation)**. It is the only option that delivers the exact UX in
[SPEC-0001](../specs/spec-0001-project-watermark.md): a real background watermark visible during task
switching. The reliance on an internal property is accepted and **isolated behind a port + adapter** (see
[ADR-0002](adr-0002-hexagonal-architecture.md)): only `IntelliJBackgroundImageAdapter` touches the unstable
API, and it fails gracefully so a broken property can never crash the IDE.

The image itself is rendered with pure JDK AWT (`java.awt` + `ImageIO`), which works headless and keeps
rendering logic in the IDE-independent core. Text is drawn fully opaque; on-screen faintness is delegated to
the IDE's background-image opacity (see [ADR-0003](adr-0003-settings-implementation.md) and
[SPEC-0003](../specs/spec-0003-settings-and-scopes.md)).

## 5. Consequences

* **Positive**:
  * Delivers the intended UX precisely; straightforward to implement and evolve.
  * Rendering is pure and unit-testable; the unstable IDE surface is a single small adapter.
* **Negative**:
  * The background-image property is internal, so the apply step may need maintenance on major IDE upgrades.
    Mitigation: it lives only in `IntelliJBackgroundImageAdapter`, which catches and logs failures.
  * Requires on-disk image management (write + cleanup); handled by `WatermarkStore` with per-project isolation.

> **Amendment 2026-07-07 (see [ADR-0005](adr-0005-branch-placeholder-implementation.md)).** The `${branch}`
> feature adds an *optional* dependency on the bundled Git plugin (Git4Idea). This stays within the
> "reasonably accessible parts of the SDK" driver: the dependency is declared `optional`, so when Git is absent
> the plugin still loads and falls back to reading `.git/HEAD`. The plugin's only *hard* dependency remains
> `com.intellij.modules.platform`.

> **Amendment 2026-07-13 — applying live under the modal Settings dialog.** Setting the background property to a
> *new* image path repaints the editor only *after* the modal Settings dialog closes: the platform loads the new
> file on a pooled thread and swaps it via a non-modal `invokeLater`, which is held back while the dialog is open
> (confirmed by decompiling the platform's `PainterHelper$MyImagePainter` — the relevant internals are identical
> on IC 2024.3.6 and 2025.2.6). So clicking **Apply** (dialog stays open) would only refresh the watermark when a
> cache race happened to fall the right way. To make Apply
> deterministic, the adapter reflectively pre-populates the platform's internal painter image cache
> (`PainterHelper$MyImagePainter.ourImageCache`) with the freshly rendered image *before* writing the property, so
> the painter's next paint takes its **synchronous** cache-hit branch and applies the watermark immediately. This
> deepens the internal-API exposure, so — consistent with this ADR — it stays strictly inside the adapter and is
> entirely best-effort: `WallpaperCacheReflection.prime` returns `false` (never throws) and the code falls back to
> the plain property write (still applied on OK/Cancel) if the internals differ on some IDE build. A guard test
> (`WallpaperCacheReflectionTest`) fails loudly if the reflected class/field/record shape changes on an upgrade.

## 6. Reflected in code

- `src/main/kotlin/.../core/ImageRenderer.kt` — AWT rendering of the transparent PNG (opaque text, antialiased,
  size adapts to the text).
- `src/main/kotlin/.../core/FontSupport.kt` — pure font-resolution policy (preferred default, `SansSerif`
  fallback) shared by the renderer, pipeline and settings UI.
- `src/main/kotlin/.../core/CoreDefaults.kt` — rendering defaults (default size 144 px, margins).
- `src/main/kotlin/.../ports/BackgroundImagePort.kt` — the port isolating the background-image concern.
- `src/main/kotlin/.../adapters/intellij/IntelliJBackgroundImageAdapter.kt` — the only code touching the
  internal `ide.background.*` properties (`IdeBackgroundUtil.EDITOR_PROP` / `FRAME_PROP`); fails gracefully.
- `src/main/kotlin/.../adapters/intellij/WallpaperCacheReflection.kt` — best-effort reflective priming of the
  IDE's internal painter image cache so **Apply** refreshes the watermark live under the modal Settings dialog;
  returns `false` and never throws on unknown internals. Guarded by
  `src/test/kotlin/.../adapters/intellij/WallpaperCacheReflectionTest.kt`.
- `src/main/kotlin/.../adapters/intellij/BackgroundPropertiesConstants.kt` — the property keys and display
  defaults (opacity 15, style `plain`, anchor `bottom_right`).
- `src/main/kotlin/.../core/WatermarkStore.kt` — on-disk write + cleanup.
- Tests: `src/test/kotlin/.../core/ImageRendererTest.kt`, `.../core/ImageRendererEdgeCasesTest.kt`.

## 7. Related documents

- **Serves**: [SPEC-0001 — Project watermark](../specs/spec-0001-project-watermark.md).
- **Related ADRs**: [ADR-0002 — Hexagonal architecture](adr-0002-hexagonal-architecture.md),
  [ADR-0003 — Settings implementation](adr-0003-settings-implementation.md),
  [ADR-0005 — Branch placeholder implementation](adr-0005-branch-placeholder-implementation.md).
