# Project Guidelines

Last updated: 2025-09-09

This document summarizes how to work on the IntelliJ Project Identifier plugin, based on the README and architectural decisions (ADR-001, ADR-002).

## 1) Project summary
- Goal: Help users distinguish between multiple open IDE projects by adding a subtle, low‑opacity text watermark to the editor background per project.
- Core flow:
  1) On project open (and when settings change), derive a short text identifier from the project name.
  2) Generate a transparent PNG image with that text and store it locally.
  3) Set the image as the editor background for the current project.
- Settings: Users can configure behavior in File | Settings | Appearance & Behavior | Appearance.

## 2) Architecture (ADR-002)
We use Hexagonal Architecture to keep business logic independent from IntelliJ SDK specifics.
- Application Core (package: `com.github.lucsartes.intellijprojectidentifierplugin.core`)
  - Stateless domain objects (e.g., `PluginSettings`, `ProjectIdentifier`).
  - Use cases/functions that implement the core rules.
  - No dependencies on IntelliJ Platform SDK.
- Ports (package: `com.github.lucsartes.intellijprojectidentifierplugin.ports`)
  - Interfaces describing what the core needs from the outside world (e.g., image rendering, background setter, settings gateway, project events).
- Adapters (package: `com.github.lucsartes.intellijprojectidentifierplugin.adapters`)
  - Implement ports for specific frameworks/SDKs.
  - Naming convention: `[FrameworkName]Adapter`, e.g., `IntelliJUiAdapter`, `AwtImageAdapter`.
- Inflow adapters are configured via `plugin.xml` (e.g., StartupActivity, Configurable), which trigger the application core.

Directory/package guidance within `src/main/kotlin/com/github/lucsartes/intellijprojectidentifierplugin`:
- `core/...` – pure Kotlin; no IntelliJ imports.
- `ports/...` – only interfaces; no IntelliJ imports.
- `adapters/intellij/...` – IntelliJ SDK dependent code.
- `adapters/awt/...` or similar – technology-specific adapters for image generation.

## 3) Technical solution choice (ADR-001) and constraints
- Strategy: Dynamic image generation of a transparent PNG, then set as editor background.
- Caveat: Relies on a private/unstable IDE property (`ide.background.image`) or equivalent internal API. This may break in future IDE versions.
- Mitigation: Keep this dependency isolated in adapters. Prefer feature flags/guards and defensive error handling; fail gracefully if the API becomes unavailable.

## 4) Coding rules and boundaries
- Do NOT reference IntelliJ SDK classes from `core` or `ports`.
- Keep `core` stateless and deterministic; functions should be easy to unit test.
- Public APIs in `ports` should be small and focused on the domain language (no UI/IDE types leak-through).
- Adapters convert between IDE/runtime types and domain types.
- Naming:
  - Ports: `*Port` or verbs as interfaces (e.g., `BackgroundImageSetter`, `ImageRenderer`).
  - Adapters: `[Framework]Adapter` suffix.
  - Use cases: clear verb names (e.g., `GenerateIdentifier`, `ApplyBackground`).
- Error handling:
  - Use sealed results or exceptions with meaningful messages at adapter boundaries.
  - Log adapter-level failures; core should not log.
- Filesystem: image generation must ensure cleanup/overwrite strategy; avoid unbounded temp growth.

## 5) Testing
- Unit tests target `core` and `ports` contracts—fast and IDE-independent.
- Adapter tests may use integration-style checks where possible and be conditionally executed if IDE APIs are present.
- Provide fakes/mocks for ports in tests.
- Add coverage for:
  - Identifier derivation rules.
  - PNG rendering params (opacity, font sizing decisions where applicable).
  - Settings change reapplication logic.

## 6) Contribution workflow
- Before coding:
  - Check existing ADRs; if you propose a significant design/tech change, add or update an ADR under `adr/` using the ADR-000 template.
- During development:
  - Respect architecture boundaries.
  - Add/adjust tests.
  - Update README if user-facing behavior changes.
- Commits/PRs:
  - Small, focused commits.
  - Reference ADRs and include rationale for adapter changes touching unstable API.
  - Provide test evidence or a manual verification note.

## 7) PR checklist
- [ ] Core logic changes confined to `core` (no IntelliJ imports).
- [ ] New capabilities modeled via `ports`, with implementations in `adapters`.
- [ ] Unstable API usage isolated and guarded; failure is graceful.
- [ ] Unit tests for core and contract tests for ports.
- [ ] README/ADR updates when required.
- [ ] No unused files left by image generation; cleanup strategy verified.

## 8) Operational notes
- On startup and on settings change, adapters should:
  - Recompute the identifier text from project name/settings.
  - Regenerate and apply the background image.
  - Handle missing permissions, I/O failures, or API unavailability without crashing the IDE.
- Store generated files in a predictable project-level or user cache location and document it for users.

## 9) References
- README.md – product overview and user instructions.
- adr/ADR-001 selection of technical solution.md – dynamic image generation decision and implications.
- adr/ADR-002 technical architecture.md – hexagonal layout and mapping to IntelliJ template.
