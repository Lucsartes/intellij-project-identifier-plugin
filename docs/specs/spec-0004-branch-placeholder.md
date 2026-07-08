# SPEC-0004: Branch placeholder

> Product/behavioral spec. Describes the user-facing behavior of the `${branch}` placeholder. The technical
> realization (branch detection strategy, refresh pipeline) lives in
> [ADR-0005](../adrs/adr-0005-branch-placeholder-implementation.md). Keep this in sync with the code
> (see [`docs/README.md`](../README.md)).

* **Status**: Accepted
* **Last updated**: 2026-07-08
* **Owners**: Project maintainers

## 1. Summary

The per-project **identifier override** (see [SPEC-0002 §3.3](spec-0002-identifier-derivation.md)) accepts a
dynamic placeholder, `${branch}`, that is replaced with the project's current Git branch. When the user
switches branches, the watermark updates automatically. This lets someone with several checkouts of the same
repo — or who moves between feature branches — tell windows apart by branch during task switching.

## 2. Context & motivation

A static override can't distinguish two windows on the *same* project sitting on *different* branches. Putting
the live branch in the watermark solves that, and it must stay correct as the user checks out other branches,
without them having to touch settings.

## 3. Behavior

**Syntax.** In the identifier override, type `${branch}` wherever the branch should appear — for example
`XXX - ${branch}`. Surrounding text is preserved as written.

**Substitution.**
- `${branch}` is replaced with the current branch name (e.g. `feature/JIRA-123`, preserved verbatim including
  slashes).
- When there is **no branch** — the project is not a Git repository, or Git is on a **detached HEAD** —
  `${branch}` resolves to an **empty string**. Surrounding text is **not** trimmed. Example: with no branch,
  `"XXX - ${branch} - YYY"` renders as `"XXX -  - YYY"`.
- Unknown placeholders (any `${...}` other than `${branch}`) are left untouched.

**Automatic refresh.** While an override contains `${branch}`, the plugin watches for branch changes and
re-generates the watermark when the branch actually changes. A change to an unrelated part of the repository
does not force a redraw. If the override does not use `${branch}`, no branch watching happens at all.

**How quickly it refreshes.** The watermark updates **instantly** on checkout when the IDE's bundled Git
integration is available; if it is disabled or absent, the plugin still tracks the branch and refreshes
**shortly after** a change (within a few seconds). Either way, the feature works — see
[ADR-0005](../adrs/adr-0005-branch-placeholder-implementation.md) for the two detection paths.

**Discoverability.** The override field carries a help tooltip (in English and French) documenting the
`${branch}` syntax and the empty-string fallback.

## 4. Non-goals / out of scope

- Only `${branch}` exists today. The mechanism is designed to grow (e.g. a future `${project}` or `${sha}`),
  but no other placeholders are supported yet.
- `${branch}` is only meaningful inside the **override**; it is not applied to automatically derived
  identifiers.
- The plugin does not create, switch, or manage Git branches — it only reads the current one.

## 5. Reflected in code

- `src/main/kotlin/.../core/TemplateResolver.kt` — pure `${name}` substitution; empty string for a null value;
  unknown placeholders preserved.
- `src/main/kotlin/.../core/BranchChangeDetector.kt` — refresh only on an actual branch change.
- `src/main/kotlin/.../core/GitHeadParser.kt` — reads the branch from `.git/HEAD` (detached HEAD → null).
- `src/main/kotlin/.../ports/BranchProvider.kt` — the branch contract used by the core/pipeline.
- `src/main/kotlin/.../adapters/intellij/GitBranchProvider.kt` — instant, event-driven detection via bundled
  Git (Git4Idea).
- `src/main/kotlin/.../adapters/intellij/FileSystemBranchProvider.kt` — dependency-free polling fallback.
- `src/main/kotlin/.../adapters/intellij/BranchWatchService.kt` — starts/stops watching based on `${branch}`
  presence.
- `src/main/resources/messages/MyBundle*.properties` — the override help tooltip (keys
  `settings.identifier.override.tooltip.*`).
- Tests: `src/test/kotlin/.../core/TemplateResolverTest.kt`, `.../core/BranchChangeDetectorTest.kt`,
  `.../core/GitHeadParserTest.kt`.

## 6. Related decisions

- **Realized by**: [ADR-0005 — Branch placeholder implementation](../adrs/adr-0005-branch-placeholder-implementation.md).
- **See also**: [SPEC-0002 — Identifier derivation](spec-0002-identifier-derivation.md),
  [SPEC-0003 — Settings & scopes](spec-0003-settings-and-scopes.md),
  [SPEC-0005 — Internationalization](spec-0005-internationalization.md).

## 7. Change history

- 2026-07-07 — Feature introduced: `${branch}` placeholder with automatic refresh and empty-string fallback.
