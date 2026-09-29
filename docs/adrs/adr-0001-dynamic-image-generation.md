# ADR-0001: Render the watermark as an image and set it as the IDE background

* **Status**: Accepted
* **Decided**: 2025-08-21
* **Last updated**: 2026-09-24

## 1. Context

[SPEC-0001](../specs/spec-0001-project-watermark.md) asks for a faint text marker behind the editor and the
empty window area, per project, that stays visible in a task switcher. The question is how to put such a marker
into the IDE's UI without getting in the way of coding and without a heavy maintenance burden.

## 2. Decision drivers

* **True background** — the marker must sit behind the code, not compete with it as an overlay.
* **Feasibility** — reachable with public, or at least accessible, parts of the IntelliJ Platform SDK.
* **Per-project scope** — each project gets its own marker.
* **API-stability risk** — internal or undocumented IDE APIs can break between releases. Keep their use small
  and isolated.
* **Performance** — no noticeable lag.

## 3. Considered options

* **A — Generate an image and set it as the IDE background image.** Render a transparent PNG with the text and
  hand it to the IDE's built-in background-image feature. Pros: a genuine background, per project, and it
  automates something users already do by hand, which proves the end state is supported. Cons: the IDE setting
  it writes is internal and may change between releases; image files must be written and cleaned up.
* **B — Paint directly on the editor canvas** with a custom painter. Pros: no files. Cons: complex custom
  painting, with the risk of drawing over code.
* **C — A separate UI element** (status bar widget, tool window). Pros: simple, stable public APIs. Cons: not a
  background, and not visible in a task switcher. It fails the core need.

## 4. Decision

Adopt **option A**.

* **Rendering stays pure.** The image is drawn with plain JDK AWT, which also works headless. Rendering
  therefore lives in the IDE-independent core and is unit-tested (see
  [ADR-0002](adr-0002-hexagonal-architecture.md)). The text is drawn fully opaque, and on-screen faintness is
  left to the IDE's background opacity.
* **The internal API is isolated.** Only the background-image adapter (behind `BackgroundImagePort`) touches
  the internal background properties. It catches every failure, so a broken property on some IDE build can
  never crash the IDE: the watermark just doesn't update.
* **The user's display choices win.** When the plugin applies an image, it keeps the opacity, fill style and
  anchor already configured and uses its own defaults only for values that aren't set yet. Display remains the
  IDE's job (see [ADR-0003](adr-0003-settings-implementation.md)).
* **Every render gets a new file name.** The IDE caches background images by path, so rewriting the same file
  would keep showing the stale image. Each render writes a uniquely named file in a per-project folder under the
  IDE's system directory and deletes that project's previous files. The per-project folder means one project's
  cleanup can never remove another project's image.

## 5. Consequences

* **Positive** — delivers exactly the intended experience. Rendering is pure and tested, and the unstable IDE
  surface is confined to one small adapter.
* **Negative** — the internal property can change on a major IDE upgrade, so the apply step may need
  maintenance. It is contained in one adapter and fails soft. On-disk image management (write + cleanup) is
  also needed.

## 6. Code pointers

* `core/ImageRenderer.kt` — the pure renderer.
* `adapters/intellij/IntelliJBackgroundImageAdapter.kt` — the only code that writes the IDE background
  properties.

## 7. Related

* **Serves**: [SPEC-0001 — Project watermark](../specs/spec-0001-project-watermark.md).
* **Related ADRs**: [ADR-0002](adr-0002-hexagonal-architecture.md) (isolation of the unstable API),
  [ADR-0003](adr-0003-settings-implementation.md) (content vs display boundary),
  [ADR-0006](adr-0006-serialized-refresh-pipeline.md) (how renders are sequenced).

## Amendments

* **2026-07-13 — Live refresh under the modal Settings dialog.** When the background property points at a new
  file, the IDE loads it in the background and swaps it in only once the modal Settings dialog closes. As a
  result, **Apply** (which keeps the dialog open) didn't reliably refresh the watermark. The adapter now puts
  the freshly rendered image into the IDE's internal background-image cache, by reflection, *before* it writes
  the property, so the next repaint shows it right away. This goes deeper into internals, so it follows the
  rules above: it lives only in the adapter, is best-effort (on any mismatch it silently falls back to the
  plain property write, which still applies when the dialog closes), and a guard test (`WallpaperCacheReflectionTest`)
  fails loudly if the internal shape changes on an IDE upgrade.
