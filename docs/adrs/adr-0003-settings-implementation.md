# ADR-0003: Settings implementation

* **Status**: Accepted
* **Decided**: 2025-09-18
* **Last updated**: 2026-09-24

## 1. Context

[SPEC-0003](../specs/spec-0003-settings-and-scopes.md) requires per-project and global settings, a clear
boundary with the IDE's Background Image page, and an automatic watermark refresh when settings change. The
technical questions: which settings the plugin owns and which it leaves to the IDE, how the two scopes are
presented and stored, and how a change reaches the watermark, all within the boundaries of
[ADR-0002](adr-0002-hexagonal-architecture.md).

## 2. Decision drivers

* **Don't fight the IDE** — don't duplicate the IDE's background display controls. They sit on an unstable
  surface and would double the maintenance.
* **Platform conventions** — standard settings pages and persistent services, with the right storage scope for
  each setting, and standard Apply/OK/Cancel semantics.
* **Automatic refresh** — a saved change must reach the watermark without a restart.
* **Hexagonal boundaries** — settings *models* are pure core types, and only adapters touch the IDE settings
  APIs.

## 3. Considered options

**What the plugin exposes:**

* **A — Everything** (content + opacity, placement, tiling). Duplicates the IDE, is fragile against IDE
  changes, and mixes concerns.
* **B — Only what shapes the image *content*** (text, font, size, color). Display stays on the IDE's Background
  Image page. A clear boundary, fewer unstable touchpoints, and a simpler UI.
* **C — Nothing.** The user can't control the text, which undermines the plugin.

**How the two scopes are presented:** two unrelated top-level pages, or one parent page (global) with a child
page (project).

## 4. Decision

* **Boundary: option B.** The plugin configures content only. A permanent hint on the project page points to
  the IDE's Background Image page for opacity and position.
* **One parent page (global) with a nested child page (per project)**, both under *Appearance & Behavior*.
* **Storage scopes.** Per-project settings go to the project's **workspace file**, which is personal and not
  meant for version control: a watermark is a personal aid for switching windows, not a team convention. Global
  settings go to an application-level file. Each is a persistent service implementing its settings port.
* **Changes travel over the message bus.** Saving settings publishes on a topic. Each open project listens,
  reruns the refresh pipeline ([ADR-0006](adr-0006-serialized-refresh-pipeline.md)), and re-checks whether
  branch watching is needed ([ADR-0005](adr-0005-branch-placeholder-implementation.md)). Settings pages never
  call the pipeline directly.
* **The live preview reuses the core.** The project page renders its preview with the same pure core pieces the
  pipeline uses (derivation, placeholder resolution, renderer), entirely in memory. It writes no file, commits
  nothing and never touches the IDE background, so it can't disturb the applied watermark. It shows content
  only, because display stays IDE-owned.
* **Everything is committed on Apply.** Both pages save only on Apply/OK. Their Reset buttons just fill the form
  with defaults. On the project page, Reset also marks the IDE display options (opacity, fill style, anchor) to
  be restored to the plugin defaults during the same Apply, before the save, so the refresh that follows uses
  the restored values. Cancel discards a pending Reset like any other edit.

## 5. Consequences

* **Positive** — a clean split (content in the plugin, display in the IDE), few fragile touchpoints, standard
  platform patterns and dialog behavior, and automatic refresh without coupling pages to the pipeline.
* **Negative** — full customization spans three places (project page, global page, IDE Background Image page),
  an accepted trade-off called out in [SPEC-0003 §4](../specs/spec-0003-settings-and-scopes.md). Per-project
  settings can't be shared with a team.
* **Watch point** — if the IDE ever removes or reworks its Background Image controls, revisit whether the
  plugin must offer compensating options. That would amend this ADR together with SPEC-0003.

## 6. Code pointers

* `adapters/intellij/IntelliJSettingsConfigurable.kt` / `IntelliJApplicationSettingsConfigurable.kt` — the
  two pages.
* `adapters/intellij/IntelliJSettingsService.kt` / `IntelliJApplicationSettingsService.kt` — persistence and
  change topics.

## 7. Related

* **Serves**: [SPEC-0003 — Settings & scopes](../specs/spec-0003-settings-and-scopes.md).
* **Related ADRs**: [ADR-0001](adr-0001-dynamic-image-generation.md) (image content vs IDE display),
  [ADR-0002](adr-0002-hexagonal-architecture.md), [ADR-0004](adr-0004-internationalization-implementation.md)
  (all labels are localized), [ADR-0006](adr-0006-serialized-refresh-pipeline.md).

## Amendments

* **2025-10-08** — Global scope added (ignored words), and the pages restructured into parent (global) and
  child (project).
* **2026-07-10** — Live preview added to the project page.
* **2026-09-24** — The project page's Reset no longer saves immediately. It now follows the same
  commit-on-Apply rule as every other edit, matching the global page. It had saved immediately since it was
  introduced, at a time when Apply couldn't refresh the background live under the modal dialog; the
  [ADR-0001](adr-0001-dynamic-image-generation.md) amendment of 2026-07-13 removed that limitation.
