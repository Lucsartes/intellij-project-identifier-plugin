# SPEC-0001: Project watermark

* **Status**: Accepted
* **Last updated**: 2026-09-24

## 1. Summary

Project Identifier helps a developer who keeps several IDE windows open tell them apart at a glance. For each
open project, the plugin shows a large, faint **text watermark** behind the editor, and behind the empty window
area shown when no file is open. The text is derived from the project, by default a short acronym of its name.
Each window therefore carries a distinct marker, still readable in a task switcher (`Alt+Tab`, the Windows key,
Mission Control, …).

## 2. Motivation

Developers who juggle many projects (microservices, several checkouts of the same repository, client projects)
lose time working out *which* window is which, because the title bar is easy to miss when switching quickly. A
large, faint marker behind the code is readable at a glance during a task switch, and faint enough not to
distract while coding.

Users could already do this by hand: make an image with some text and set it as the IDE background. But that
is a fiddly chore to repeat for every project and redo on every change. The plugin automates it.

## 3. Behavior

### 3.1 When the watermark is (re)generated

The plugin recomputes and reapplies a project's watermark automatically:

- when the project is opened;
- when the plugin's per-project or global settings are applied (see [SPEC-0003](spec-0003-settings-and-scopes.md));
- when the current Git branch changes, if the identifier uses the `${branch}` placeholder (see
  [SPEC-0004](spec-0004-branch-placeholder.md)).

No restart or manual refresh is ever needed.

### 3.2 What the user sees

- The identifier text appears as the background of the editor and of the empty window area, **for this project
  only**. Other open projects keep their own watermark.
- The text content, font, size and color follow [SPEC-0002](spec-0002-identifier-derivation.md) and the
  user's settings.
- How the watermark is *displayed* (opacity, position, scaling) is controlled by the IDE's own Background Image
  settings. The first time the plugin applies a watermark to a project, it uses a low opacity and places the
  text in the bottom-right corner, unscaled. After that, it keeps whatever the user sets on the IDE's
  Background Image page.
- The watermark is an ordinary IDE background image, so the user can change or clear it on the IDE's
  Background Image page. The plugin sets it again at the next regeneration (see §3.1), for example when the
  project is reopened.

### 3.3 Edge cases

- If the identifier text is empty (for example a blank project name), no watermark text is shown, and nothing
  fails.
- A problem while generating or applying the watermark never blocks, slows down or crashes the IDE. The
  watermark is simply not updated, and the problem is recorded in the IDE log.

## 4. Out of scope

- **No display controls in the plugin.** Opacity, position, scaling and tiling belong to the IDE's
  *Appearance & Behavior | Appearance | Background Image* page. The plugin decides only *what* the image
  shows (see [SPEC-0003 §3.3](spec-0003-settings-and-scopes.md)).
- **No overlays.** The plugin never draws over the code or adds UI components. The marker is only ever a
  background.
- **No on/off switch.** To remove the watermark permanently, disable the plugin.

## 5. Related

- **Realized by**: [ADR-0001 — Dynamic image generation](../adrs/adr-0001-dynamic-image-generation.md),
  [ADR-0006 — Serialized refresh pipeline](../adrs/adr-0006-serialized-refresh-pipeline.md),
  [ADR-0002 — Hexagonal architecture](../adrs/adr-0002-hexagonal-architecture.md).
- **See also**: [SPEC-0002 — Identifier derivation](spec-0002-identifier-derivation.md),
  [SPEC-0003 — Settings & scopes](spec-0003-settings-and-scopes.md),
  [SPEC-0004 — Branch placeholder](spec-0004-branch-placeholder.md).
