# SPEC-0003: Settings & scopes

* **Status**: Accepted
* **Last updated**: 2026-09-24

## 1. Summary

The plugin has a small set of settings in two **scopes**: per-project settings and global settings that apply
to every project. Both live under *Settings | Appearance & Behavior | Project Identifier Settings*. The plugin
configures only the *content* of the watermark. How it is *displayed* (opacity, position, scaling) stays on the
IDE's own Background Image page.

## 2. Motivation

Two forces shape the settings:

1. **Don't duplicate the IDE.** The IDE already has a complete Background Image page (opacity, placement,
   scaling, anchor). Offering the same controls again would duplicate and conflict with it. The plugin limits
   itself to what only it can do, the image content, and points users to the IDE page for the rest.
2. **Per-project vs global.** Some choices naturally belong to one project (its override, font, size, color).
   Others are set once for everyone (words to ignore when building identifiers).

## 3. Behavior

The settings are split across two pages:

- **Project Identifier Settings** (parent page): the **global** settings.
- **Project Identifier Settings | Project Settings** (child page): the settings of the **current project**.

Both pages follow the usual IDE rules: **Apply** or **OK** saves, **Cancel** discards what hasn't been applied
yet. Applying a change regenerates the affected watermark(s) immediately, even while the settings window stays
open. A global change updates every open project.

### 3.1 Per-project settings (child page)

These settings are saved per project and **for the current user only**. They are not shared with teammates
through version control.

| Setting                 | What it does                                                                                           | Default |
|-------------------------|--------------------------------------------------------------------------------------------------------|---------|
| **Identifier override** | Replaces the automatic acronym with custom text, which may contain `${branch}` (see [SPEC-0002 §3.3](spec-0002-identifier-derivation.md), [SPEC-0004](spec-0004-branch-placeholder.md)). A help tooltip explains the placeholder. | empty (automatic acronym) |
| **Font family**         | Typeface of the text, picked from a short list of common fonts installed on the machine. A missing font falls back to a generic sans-serif. | JetBrains Mono |
| **Text size (px)**      | Font size, picked from a list of preset sizes.                                                          | 144 px  |
| **Text color**          | Color of the text.                                                                                      | White   |

The page also offers:

- **A live preview.** It shows the identifier with the chosen text, font, size and color, updated as you type,
  so options can be compared before anything is saved. It shows the content at full opacity over the IDE's
  background color, because the real on-screen opacity and position come from the IDE's Background Image page.
- **A Reset button.** It fills the page with the defaults above and also marks the watermark's display options
  (opacity, position, fill style) for reset to the plugin's defaults (see §3.3). Like any other edit, this
  takes effect on **Apply/OK**, and **Cancel** discards it.
- **A permanent hint** pointing to the IDE's Background Image page for opacity and position.

### 3.2 Global settings (parent page)

These settings are saved once for the whole IDE installation and apply to every project:

| Setting           | What it does                                                                                                          | Default |
|-------------------|-----------------------------------------------------------------------------------------------------------------------|---------|
| **Ignored words** | Comma-separated, case-insensitive words removed from project names before the acronym is built (see [SPEC-0002 §3.2](spec-0002-identifier-derivation.md)). A help tooltip gives an example. | empty   |

The page has its own **Reset** button, which fills the page with the defaults. As on the project page, it takes
effect on **Apply/OK**.

### 3.3 Where opacity, position and scaling live

These are **not** plugin settings. To change how faint the watermark is, or where it sits, the user goes to
*Appearance & Behavior | Appearance | Background Image*. The first time the plugin applies a watermark to a
project, it uses a low opacity and the bottom-right corner, unscaled. From then on, the IDE page is the source of
truth: the plugin keeps whatever the user sets there, unless the user applies the per-project **Reset**.

## 4. Out of scope

- The plugin will not add opacity, placement, scaling or anchor controls. That would duplicate the IDE (see
  [ADR-0003](../adrs/adr-0003-settings-implementation.md)).
- Accepted trade-off: fully customizing the watermark can involve **three** places: the project page, the global
  page, and the IDE's Background Image page.
- There is no way to share per-project settings with a team.

## 5. Related

- **Realized by**: [ADR-0003 — Settings implementation](../adrs/adr-0003-settings-implementation.md).
- **See also**: [SPEC-0001](spec-0001-project-watermark.md), [SPEC-0002](spec-0002-identifier-derivation.md),
  [SPEC-0004](spec-0004-branch-placeholder.md), [SPEC-0005](spec-0005-internationalization.md).
