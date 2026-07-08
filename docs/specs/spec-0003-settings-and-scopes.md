# SPEC-0003: Settings & scopes

> Product/behavioral spec. Describes *what* the user can configure and *where*. The technical realization
> (configurables, persistent services, storage files) lives in
> [ADR-0003](../adrs/adr-0003-settings-implementation.md). Keep this in sync with the code
> (see [`docs/README.md`](../README.md)).

* **Status**: Accepted
* **Last updated**: 2026-07-08
* **Owners**: Project maintainers

## 1. Summary

The plugin exposes a small set of settings, organized by **scope**: some apply to a single project, some apply
globally to every project. They are reached under *File | Settings | Appearance & Behavior*. The plugin
deliberately configures only the *content* of the watermark image; everything about how the image is
*displayed* (opacity, position, scaling) is left to the IDE's own Background Image page.

## 2. Context & motivation

Two forces shape the settings:

1. **Plugin vs IDE responsibilities.** The IDE already has a rich Background Image UI (opacity, placement,
   tiling, anchor). Re-exposing those would duplicate and fight the IDE. So the plugin limits itself to what
   only it can do — decide the image content — and points users at the IDE page for display tweaks.
2. **Per-project vs global.** Some choices are naturally per-project (this project's override, its font/size/
   color); others are naturally global (words to ignore when deriving identifiers, configured once).

## 3. Behavior

Settings live under two nested menu entries in *Appearance & Behavior*:

- **Project Identifier Settings** (parent) → the **global** settings.
- **Project Identifier Settings | Project Settings** (child) → the **per-project** settings.

Changing any setting re-generates the affected watermark(s) automatically — no restart, no manual refresh.

### 3.1 Per-project settings (child menu)

Stored with the project, so they travel with it and differ per project:

| Setting              | What it does                                                                 | Default |
|----------------------|------------------------------------------------------------------------------|---------|
| **Identifier override** | Replaces the derived identifier with custom text; supports `${branch}` (see [SPEC-0004](spec-0004-branch-placeholder.md)). A help tooltip documents the placeholder syntax. | unset (derive from name) |
| **Font family**      | Typeface used to render the text; a curated dropdown of common fonts. Unavailable fonts fall back gracefully. | JetBrains Mono if available, else a sans-serif fallback |
| **Text size (px)**   | Font size, chosen from a preset list.                                        | 144 px  |
| **Text color**       | Color of the rendered text.                                                  | White   |

The child page also has a **Reset** action that returns these per-project settings to their defaults and
restores the IDE background display options (opacity/style/anchor) to the plugin's defaults, and a permanent
**hint** pointing to the IDE Background Image page for opacity/position.

### 3.2 Global settings (parent menu)

Stored once for the whole IDE installation and applied to all projects:

| Setting          | What it does                                                                         | Default |
|------------------|--------------------------------------------------------------------------------------|---------|
| **Ignored words**| Comma-separated, case-insensitive words removed from a project name before deriving its identifier (see [SPEC-0002 §3.2](spec-0002-identifier-derivation.md)). | empty   |

The **Ignored words** field has a help tooltip with an example, and the parent page provides its own **Reset**
action.

### 3.3 Where opacity/position/scaling live

These are **not** plugin settings. To change how faintly or where the watermark shows, the user goes to
*Appearance & Behavior | Appearance | Background Image*. The first time the plugin applies an image it seeds a
low default on-screen opacity, but from then on the IDE page is the source of truth and the plugin preserves
whatever the user has set there.

## 4. Non-goals / out of scope

- The plugin will not add opacity/placement/tiling/anchor controls; that would duplicate the IDE and is
  explicitly rejected (see [ADR-0003](../adrs/adr-0003-settings-implementation.md)).
- Consequence the user should expect: full customization may span **three** places — per-project plugin
  settings, global plugin settings, and the IDE Background Image page. This is an accepted trade-off in
  exchange for not fighting the IDE.

## 5. Reflected in code

- `src/main/kotlin/.../core/ProjectSettings.kt` — per-project settings model (override, font, size, color).
- `src/main/kotlin/.../core/ApplicationSettings.kt` — global settings model (ignored words).
- `src/main/kotlin/.../adapters/intellij/IntelliJSettingsConfigurable.kt` — per-project settings UI (curated
  font list, size presets, color picker, reset, hint).
- `src/main/kotlin/.../adapters/intellij/IntelliJApplicationSettingsConfigurable.kt` — global settings UI.
- `src/main/kotlin/.../adapters/intellij/IntelliJSettingsService.kt` /
  `.../IntelliJApplicationSettingsService.kt` — persistence + change notifications.
- `src/main/resources/messages/MyBundle*.properties` — all user-facing labels, hints and tooltips.
- `src/main/resources/META-INF/plugin.xml` — nested `applicationConfigurable` + `projectConfigurable` wiring.
- Tests: `src/test/kotlin/.../adapters/intellij/IntelliJApplicationSettingsServiceTest.kt`,
  `.../core/ProjectSettingsTest.kt`, `.../core/ApplicationSettingsTest.kt`.

## 6. Related decisions

- **Realized by**: [ADR-0003 — Settings implementation](../adrs/adr-0003-settings-implementation.md).
- **See also**: [SPEC-0001](spec-0001-project-watermark.md), [SPEC-0002](spec-0002-identifier-derivation.md),
  [SPEC-0004](spec-0004-branch-placeholder.md), [SPEC-0005](spec-0005-internationalization.md).

## 7. Change history

- 2025-10-08 — Settings reorganized into a nested parent (global) / child (per-project) structure; global
  ignored-words setting added.
- 2025-10-01 — Initial per-project settings under *Appearance & Behavior | Appearance*.
