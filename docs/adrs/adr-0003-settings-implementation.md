# ADR-0003: Settings implementation

> Technical decision. Serves [SPEC-0003 — Settings & scopes](../specs/spec-0003-settings-and-scopes.md).
> Living document — keep in sync with the code (see [`docs/README.md`](../README.md)).

* **Status**: Accepted
* **Last updated**: 2026-07-08 (originally decided 2025-09-18)
* **Authors**: Project maintainers

## 1. Context

[SPEC-0003](../specs/spec-0003-settings-and-scopes.md) requires two scopes of settings (per-project and
global), a clear boundary against the IDE's Background Image page, and automatic re-generation when settings
change. The technical questions: what settings does the plugin own vs defer to the IDE, how are the two scopes
implemented and persisted, and how do changes trigger a refresh — all while respecting the hexagonal boundaries
of [ADR-0002](adr-0002-hexagonal-architecture.md).

## 2. Decision drivers

* **Respect hexagonal boundaries** — settings *models* stay in `core`; only adapters touch IDE settings APIs.
* **Don't fight the IDE** — avoid duplicating the IDE's background-image display controls (unstable surface,
  extra maintenance).
* **Platform conventions** — use standard `Configurable` + `PersistentStateComponent` patterns and the correct
  storage scope for each setting.
* **Automatic refresh** — a settings change must re-run the watermark pipeline without a restart.

## 3. Considered options

**What to expose (the plugin/IDE boundary):**
* **A — Expose everything** (text + font + size + color + opacity + placement + tiling). Cons: duplicates IDE
  settings, fragile against IDE changes, violates separation of concerns.
* **B — Expose only image-*content* controls** (identifier text, font, size, color); defer opacity/placement/
  tiling/anchor to the IDE Background Image page. Pros: clear boundary, fewer unstable touchpoints, simpler UI.
* **C — Expose nothing**, rely on IDE presets. Cons: can't control the text — undermines the plugin's purpose.

**How to present the two scopes:**
* Two separate top-level menu items, **or** a nested parent (global) → child (project) structure.

## 4. Decision

Adopt **Option B** for the boundary, and a **nested configurable** structure for the scopes.

* **Boundary.** The plugin configures only what shapes the *image content*. Opacity, placement, scaling, tiling
  and anchor remain under *Appearance & Behavior | Appearance | Background Image*. A permanent hint in the
  plugin UI points users there.
* **UI structure.** A parent `applicationConfigurable` ("Project Identifier Settings", `parentId="appearance"`)
  hosts the **global** settings; a `projectConfigurable` nested under it (via `parentId`) hosts the
  **per-project** settings. Titles come from the resource bundle (`settings.parent.title`,
  `settings.child.title`).
* **Persistence.** Two `PersistentStateComponent` services implement the `core` ports:
  * `IntelliJSettingsService` (project-level) implements `ProjectSettingsPort`, stored in the **workspace file**
    (`StoragePathMacros.WORKSPACE_FILE`), state name `ProjectIdentifierSettings`. Blank strings are normalized
    to null on the way in and out.
  * `IntelliJApplicationSettingsService` (application-level) implements `ApplicationSettingsPort`, stored in the
    application config file `projectIdentifier.xml`, state name `ProjectIdentifierApplicationSettings`.
* **Change propagation.** Each service publishes on a message-bus `Topic` when settings are saved (the
  application service only publishes when the state actually changed). `ProjectStartupActivity` subscribes to
  both topics — tied to the pipeline service's lifetime — and calls `WatermarkPipelineService.rerun()` (and
  `BranchWatchService.sync()` for project changes). This is what makes the watermark refresh automatically.
* **Reset.** The per-project page's Reset restores `ProjectSettings()` defaults and calls
  `BackgroundImagePort.resetBackgroundSettingsToDefaults()` so the IDE display options return to the plugin's
  defaults too.
* **Defaults (in code).** Font size default 144 px; text color default white; font default "JetBrains Mono"
  when the JRE reports it available, otherwise the core's `SansSerif` fallback. The font dropdown shows a
  curated list filtered by locally available families (rendering falls back gracefully for missing fonts).

## 5. Consequences

* **Positive**: clean separation (content in plugin, display in IDE); fewer fragile IDE-API touchpoints;
  standard platform patterns; global preferences configured once; automatic refresh via message bus.
* **Negative**: full customization can span three places (per-project plugin, global plugin, IDE Background
  Image) — an accepted trade-off called out in [SPEC-0003 §4](../specs/spec-0003-settings-and-scopes.md);
  two services/configurables add a little structure.
* **Neutral**: two domain models (`ProjectSettings`, `ApplicationSettings`), two ports, two adapters — matching
  the hexagonal layout.

## 6. Reflected in code

- `src/main/kotlin/.../core/ProjectSettings.kt`, `.../core/ApplicationSettings.kt` — the two settings models.
- `src/main/kotlin/.../ports/ProjectSettingsPort.kt`, `.../ports/ApplicationSettingsPort.kt` — the settings ports.
- `src/main/kotlin/.../adapters/intellij/IntelliJSettingsService.kt` — project persistence (workspace file) +
  `TOPIC`.
- `src/main/kotlin/.../adapters/intellij/IntelliJApplicationSettingsService.kt` — application persistence
  (`projectIdentifier.xml`) + `TOPIC`.
- `src/main/kotlin/.../adapters/intellij/IntelliJSettingsConfigurable.kt` — per-project UI (override + help
  tooltip, curated font list, size presets, color, reset, hint).
- `src/main/kotlin/.../adapters/intellij/IntelliJApplicationSettingsConfigurable.kt` — global UI (ignored
  words).
- `src/main/kotlin/.../adapters/intellij/SettingsUiSupport.kt` — shared Swing helpers (`labeled`,
  `restrictWidth`) used by both configurables.
- `src/main/kotlin/.../adapters/intellij/ProjectStartupActivity.kt` — subscribes to both topics and re-runs the
  pipeline.
- `src/main/kotlin/.../adapters/intellij/BackgroundPropertiesConstants.kt` — IDE display defaults used by reset.
- `src/main/resources/META-INF/plugin.xml` — nested `applicationConfigurable` + `projectConfigurable`.
- Tests: `src/test/kotlin/.../adapters/intellij/IntelliJApplicationSettingsServiceTest.kt`,
  `.../core/ProjectSettingsTest.kt`, `.../core/ApplicationSettingsTest.kt`.

## 7. Related documents

- **Serves**: [SPEC-0003 — Settings & scopes](../specs/spec-0003-settings-and-scopes.md).
- **Related ADRs**: [ADR-0001](adr-0001-dynamic-image-generation.md) (image content vs IDE display),
  [ADR-0002](adr-0002-hexagonal-architecture.md), [ADR-0004](adr-0004-internationalization-implementation.md)
  (all labels are localized).

## 8. Future-proofing

If the IDE changes or removes its Background Image controls, revisit whether the plugin should surface
compensating options. Any such change must preserve the architecture boundaries, prefer minimal duplication and
graceful degradation, and be captured by amending this ADR (or a superseding one) together with
[SPEC-0003](../specs/spec-0003-settings-and-scopes.md).
