# Settings page scope and behavior

- Status: Accepted
- Date: 2025-09-18 (updated 2025-10-08)
- Authors: Project maintainers

## 1. Context

The plugin adds a subtle text watermark to the editor and empty frame backgrounds to help distinguish multiple open IDE projects. We expose settings pages to let users control how the watermark image is generated.

There are multiple concerns to address:

1. **Plugin vs IDE responsibilities**: There is a potential overlap between what the plugin can configure (image generation) and what the IDE already provides in its Background Image UI (opacity, placement, tiling, anchor, etc.).

2. **Project-level vs Application-level settings**: Some settings naturally apply per-project (e.g., identifier override for a specific project), while others should apply globally across all projects (e.g., a list of common prefixes to ignore when generating identifiers).

We need to clarify:
- Which settings live in the plugin vs the IDE's Background Image page
- Which plugin settings are project-scoped vs application-scoped
- How to present these settings in a way that is clear and follows IntelliJ Platform conventions

Related architecture decisions:

- ADR-001 selection of a technical solution — we generate a transparent PNG and set it as the editor and empty frame background.
- ADR-002 technical architecture — core/ports/adapters separation, minimizing dependency on unstable IDE APIs.

## 2. Decision Drivers

- Respect the Hexagonal Architecture boundaries; keep core free of IDE concerns.
- Avoid duplicating or fighting existing IDE Background Image controls.
- Provide simple, predictable image-generation controls in the plugin.
- Minimize future breakage from IntelliJ changes to background image behavior.
- Keep UI lean and non-intrusive.
- Follow IntelliJ Platform conventions for settings scope (project vs application level).
- Make it immediately clear to users which settings apply to the current project vs all projects.
- Allow users to configure global preferences (e.g., ignored words) once rather than per-project.

## 3. Considered Options

- Option A: Expose all controls (text, font, size, color, opacity, placement, tiling) in the plugin.
    - Pros: One-stop shop for users.
    - Cons: Duplicates IDE settings; higher maintenance; more fragile against IDE changes; violates separation of concerns.

- Option B: Limit plugin settings strictly to image-generation aspects (text content and rendering), and defer opacity/placement/tiling to the IDE Background Image page.
    - Pros: Clear responsibility boundary; fewer unstable API touchpoints; simpler UI; aligns with ADR-001/002.
    - Cons: Users must visit two places if they want to tweak opacity/placement.

- Option C: Hide all plugin settings and rely entirely on IDE Background Image page via presets.
    - Pros: Minimal plugin UI.
    - Cons: Cannot control text content or its rendering; undermines plugin’s core value.

## 4. Decision

Adopt Option B.

The plugin’s settings page will only contain options that affect the generated watermark image itself. 
All controls related to how the IDE displays a background image (opacity, placement/anchor, scaling/tiling, scrolling behavior) remain under the existing IDE menu: File | Settings | Appearance & Behavior | Appearance | Background Image.

We will include a permanent hint in the plugin settings page directing users to that IDE menu for opacity/placement adjustments.

### 4.1. Project-Level vs Application-Level Settings (updated 2025-10-08)

Additionally, we separate plugin settings into two scopes, presented in a nested UI for clarity:

**Parent Menu: Project Identifier Settings**
- This is the main entry point for all plugin settings.
- Clicking it directly opens the **Application-Level (Global) Settings**.
- Location: `File | Settings | Appearance & Behavior | Project Identifier Settings`
- Implemented via a parent `applicationConfigurable`.

**Child Menu: Project Settings**
- This menu item is nested under the parent.
- Clicking it opens the **Project-Level Settings**.
- Location: `File | Settings | Appearance & Behavior | Project Identifier Settings | Project Settings`
- Implemented via a `projectConfigurable` nested inside the parent `applicationConfigurable`.

**Application-Level (Global) Settings**:
- Settings that should apply globally across all projects (e.g., list of words to ignore during identifier generation).
- Stored in IDE application-level configuration.
- Implemented via `IntelliJApplicationSettingsService` (application-scoped service).

**Project-Level Settings**:
- Settings that naturally vary per project (e.g., identifier override for a specific project).
- Stored in project workspace file (`.idea/workspace.xml`).
- Implemented via `IntelliJSettingsService` (project-scoped service).

**Rationale for a Nested UI**:

We previously used two separate top-level menu items but have consolidated them into a single, nested structure.

1.  **Improved Discoverability**: A single "Project Identifier Settings" entry is cleaner and easier for users to find under `Appearance & Behavior`. It groups all related settings logically.

2.  **Clear Hierarchy**: The parent menu naturally defaults to global settings, which are typically configured once. The child menu provides access to project-specific overrides, reinforcing the "global vs. local" relationship.

3.  **IntelliJ Platform Convention**: While separate configurables are common, nesting them is also a standard pattern for plugins that have both application and project scopes. This approach provides a more organized user experience.

4.  **Example Use Case**: A developer can configure globally ignored words (e.g., "TR", "TRE") under "Project Identifier Settings". For a specific project needing a custom name, they can navigate to the "Project Settings" child menu and set an identifier override.

## 5. Current Plugin Settings (as of 2025-10-08)

### 5.1. Project-Level Settings (Child Menu)

These settings are configured per-project and stored in the project's workspace file:

- **Identifier override** (optional)
    - What it does: Overrides the automatically derived project identifier text for this specific project.
    - Default: unset (derive from project name using application-level rules).
    - Scope: Project-specific.
    - Placeholders (added 2026-07-07, see ADR-005): the override text supports dynamic placeholders of the
      form `${name}`. Currently `${branch}` is substituted with the current Git branch name and the watermark
      refreshes automatically when the branch changes. When there is no branch (non-Git project or detached
      HEAD), `${branch}` is replaced with an empty string (surrounding text is preserved, not trimmed). The
      override field carries a `HelpTooltip` documenting the supported placeholders and this fallback.

- **Font family** (optional)
    - What it does: Selects the typeface used to render the watermark text. UI shows a curated list; core will fall back to a reasonable default (e.g., JetBrains Mono or SansSerif) if the requested font is unavailable.
    - Default: implementation default (no explicit value stored).
    - Scope: Project-specific.

- **Text size (px)** (optional)
    - What it does: Sets the font size in pixels for rendering the watermark text.
    - Default: implementation default (e.g., 144 px; no explicit value stored if default selected).
    - Scope: Project-specific.

- **Text color (ARGB)** (optional)
    - What it does: Sets the text color used for rendering the watermark (alpha channel within the PNG will be fully opaque for the text; overall on-screen opacity is still governed by the IDE Background Image settings).
    - Default: white (no explicit value stored if default selected).
    - Scope: Project-specific.

These settings are configured globally and apply to all projects:
    - What it does: A list of words/tokens to ignore when automatically generating project identifiers. Matching is case-insensitive. For example, if "TR" and "TRE" are in the ignored list, a project named "tr-my-project" will generate the identifier "MP" instead of "TMP".
- **Ignored words** (optional)
    - What it does: A list of words to remove from the project name before generating the identifier. This is useful for stripping common prefixes (e.g., "tr-", "project-"). Matching is case-insensitive.
    - Default: empty list.
    - Scope: Application-wide.
    - Default: empty list (no words ignored).
    - Scope: Global (all projects).
    - UI format: Words are entered as comma-separated values (CSV) in a compact, expandable text field. Each value is trimmed of whitespace, and empty values are ignored. This format is chosen for space efficiency as we don't expect a large number of exclusions.
    - Use case: Organizations often use common prefixes for project names (e.g., "tr-", "tre-", "internal-"). Adding these to the ignored list ensures identifiers are derived from meaningful parts of the project name.

### 5.3. Notes

- No plugin setting controls the watermark opacity or placement on screen. Those are IDE responsibilities (File | Settings | Appearance & Behavior | Appearance | Background Image).
- Project-level settings override or customize the behavior for individual projects.
- Application-level settings provide defaults and preprocessing rules applied to all projects.

## 6. Consequences

- Positive
    - Clear separation of concerns: image generation in plugin; display characteristics in IDE.
    - Lower maintenance burden and fewer fragile dependencies on internal/unstable IDE APIs.
    - Consistent UX by reusing the IDE Background Image page for opacity/placement.
    - Follows IntelliJ Platform conventions for settings scope (project vs application configurables).
    - Users can configure global preferences (ignored words) once and have them apply to all projects.
    - Clear visual indication in Settings UI of which settings apply per-project vs globally.
    - Enables organizational workflows where common project naming patterns can be handled consistently.

- Negative
    - Users may need to visit three places for full customization:
        1. Plugin project-level settings for per-project identifier/font/color/size
        2. Plugin application-level settings for global ignored words
        3. IDE Background Image page for opacity/placement/tiling
    - Slightly more complex architecture with two settings services and configurables.

- Neutral/Implementation details
    - The plugin settings UI includes a hint linking users to the IDE Background Image page for opacity/placement.
    - Core remains IDE-agnostic; adapters handle applying the generated image and, where needed, resetting IDE background settings to defaults.
    - Two separate domain models (`PluginSettings` for project, `ApplicationSettings` for global).
    - Two separate ports and adapters following hexagonal architecture principles.
    - Application-level settings stored in IDE application configuration, project-level in workspace file.

## 7. Future-proofing and Evolution

- The set of available settings in the plugin may evolve as IntelliJ IDEA changes its Background Image settings page. If the IDE removes or alters certain display controls, we may revisit whether the plugin should surface compensating options. Any such change must:
    - Preserve the architecture boundaries (core free of IDE types; only adapters talk to IDE APIs).
    - Be captured in a follow-up ADR that supersedes or amends this one.
    - Prefer minimal duplication of IDE features and graceful degradation when IDE APIs change.

- If IntelliJ modifies or deprecates the Background Image UI, we will assess:
    - Whether our hint text and documentation need updates.
    - Whether additional plugin-side image-generation options are warranted to maintain usability without overstepping into IDE responsibilities.

## 8. Related Documents

- README.md — user overview and instructions.
- ADR-001 selection of technical solution.md
- ADR-002 technical architecture.md
