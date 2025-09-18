# Settings page scope and behavior

- Status: Accepted
- Date: 2025-09-18
- Authors: Project maintainers

## 1. Context

The plugin adds a subtle text watermark to help distinguish multiple open IDE projects. We expose a settings page under File | Settings | Appearance & Behavior | Project Identifier to let users control how the watermark image is generated. 
There is a potential overlap between what the plugin can configure (image generation) and what the IDE already provides in its Background Image UI (opacity, placement, tiling, anchor, etc.). 
We need to clarify which settings live where, document the current set of plugin settings, and constrain future additions to keep responsibilities clear and resilient to IDE changes.

Related architecture decisions:

- ADR-001 selection of a technical solution — we generate a transparent PNG and set it as the editor background.
- ADR-002 technical architecture — core/ports/adapters separation, minimizing dependency on unstable IDE APIs.

## 2. Decision Drivers

- Respect the Hexagonal Architecture boundaries; keep core free of IDE concerns.
- Avoid duplicating or fighting existing IDE Background Image controls.
- Provide simple, predictable image-generation controls in the plugin.
- Minimize future breakage from IntelliJ changes to background image behavior.
- Keep UI lean and non-intrusive.

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

## 5. Current Plugin Settings (as of 2025-09-18)

- Identifier override (optional)
    - What it does: Overrides the automatically derived project identifier text.
    - Default: unset (derive from project name).

- Font family (optional)
    - What it does: Selects the typeface used to render the watermark text. UI shows a curated list; core will fall back to a reasonable default (e.g., JetBrains Mono or SansSerif) if the requested font is unavailable.
    - Default: implementation default (no explicit value stored).

- Text size (px) (optional)
    - What it does: Sets the font size in pixels for rendering the watermark text.
    - Default: implementation default (e.g., 144 px; no explicit value stored if default selected).

- Text color (ARGB) (optional)
    - What it does: Sets the text color used for rendering the watermark (alpha channel within the PNG will be fully opaque for the text; overall on-screen opacity is still governed by the IDE Background Image settings).
    - Default: white (no explicit value stored if default selected).

Note: No plugin setting controls the watermark opacity or placement on screen. Those are IDE responsibilities.

## 6. Consequences

- Positive
    - Clear separation of concerns: image generation in plugin; display characteristics in IDE.
    - Lower maintenance burden and fewer fragile dependencies on internal/unstable IDE APIs.
    - Consistent UX by reusing the IDE Background Image page for opacity/placement.

- Negative
    - Users may need to visit two places for full customization (plugin page for text/font/color/size; IDE page for opacity/placement/tiling).

- Neutral/Implementation details
    - The plugin settings UI includes a hint linking users to the IDE Background Image page for opacity/placement.
    - Core remains IDE-agnostic; adapters handle applying the generated image and, where needed, resetting IDE background settings to defaults.

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
