# SPEC-0002: Identifier derivation

> Product/behavioral spec. Describes *what* text ends up in the watermark. The technical realization lives in
> the ADRs linked in §6. Keep this document in sync with the code (see [`docs/README.md`](../README.md)).

* **Status**: Accepted
* **Last updated**: 2026-07-08
* **Owners**: Project maintainers

## 1. Summary

The watermark text — the *identifier* — is normally a short acronym derived automatically from the project
name. The user can influence it in two ways: by maintaining a global list of **ignored words** stripped before
derivation, and by setting a per-project **override** that replaces the derived text entirely (optionally with
dynamic placeholders). This spec defines exactly how the final identifier text is produced.

## 2. Context & motivation

A full project name is too long to sit behind the code as a glanceable marker, and many organizations prefix
project names with boilerplate (`tr-`, `internal-`, …) that carries no distinguishing value. A short acronym of
the *meaningful* parts of the name is compact and recognizable. Some projects need a hand-picked marker
instead, so an override is offered.

## 3. Behavior

### 3.1 Automatic derivation (the default)

1. The project name is split into **tokens** — maximal runs of letters or digits. Everything else (spaces,
   hyphens, underscores, punctuation) is a separator. Letters and digits are recognized across all scripts, not
   just ASCII (Unicode-aware).
2. Any token that exactly matches an **ignored word** (case-insensitive) is dropped (see §3.2).
3. The identifier is the **first character of each remaining token, uppercased**, concatenated in order.

Worked examples (no ignored words):

| Project name            | Identifier |
|-------------------------|------------|
| `My Awesome Project`    | `MAP`      |
| `  many   spaces  here `| `MSH`      |
| `foo-bar_baz`           | `FBB`      |
| `shop-api`              | `SA`       |

Edge case: a **blank** project name yields an **empty** identifier (and therefore a blank watermark).

### 3.2 Ignored words (global)

- A global, comma-separated list of words to remove from the token stream before building the acronym.
- Matching is **case-insensitive** and matches **whole tokens** only.
- Example: with ignored words `tr, tre`, the project `tr-my-project` derives `MP` (not `TMP`).
- Default: empty (nothing ignored).
- This list is application-wide — see [SPEC-0003 §3.2](spec-0003-settings-and-scopes.md).

### 3.3 Per-project override

- If a project sets an **identifier override**, that text is used verbatim instead of the derived acronym
  (ignored words and the acronym rule are not applied to it).
- The override may contain **placeholders** of the form `${name}` that are filled in automatically. The only
  placeholder currently supported is `${branch}` — see [SPEC-0004](spec-0004-branch-placeholder.md). Unknown
  `${...}` sequences are left untouched.
- Default: unset (fall back to automatic derivation).
- The override is per-project — see [SPEC-0003 §3.1](spec-0003-settings-and-scopes.md).

### 3.4 Rendering of the final text

Once the identifier text is resolved, it is rendered into the watermark image using the per-project font
family, size, and color settings, with sensible defaults. Those rendering settings and their defaults are
specified in [SPEC-0003 §3.1](spec-0003-settings-and-scopes.md); this spec only governs the *text content*.

## 4. Non-goals / out of scope

- This spec does not define *where* or *how faintly* the text appears — that is the IDE's background-image
  display (see [SPEC-0001](spec-0001-project-watermark.md) and [SPEC-0003](spec-0003-settings-and-scopes.md)).
- No stemming, fuzzy matching, or partial-token matching of ignored words — matching is exact, whole-token,
  case-insensitive.

## 5. Reflected in code

- `src/main/kotlin/.../core/IdentifierGenerator.kt` — tokenization, ignored-word filtering, acronym building.
- `src/main/kotlin/.../core/CoreDefaults.kt` — the token regex (`[\p{L}\p{N}]+`).
- `src/main/kotlin/.../core/TemplateResolver.kt` — placeholder substitution in the override.
- `src/main/kotlin/.../adapters/intellij/WatermarkPipelineService.kt` — chooses override vs derived, then
  resolves placeholders.
- Tests: `src/test/kotlin/.../core/IdentifierGeneratorTest.kt`,
  `.../core/IdentifierGeneratorIgnoredWordsTest.kt`, `.../core/IdentifierGeneratorUnicodeTest.kt`,
  `.../core/TemplateResolverTest.kt`.

## 6. Related decisions

- **Realized by**: [ADR-0002 — Hexagonal architecture](../adrs/adr-0002-hexagonal-architecture.md) (the
  derivation is pure core logic), [ADR-0005 — Branch placeholder implementation](../adrs/adr-0005-branch-placeholder-implementation.md).
- **See also**: [SPEC-0001](spec-0001-project-watermark.md), [SPEC-0003](spec-0003-settings-and-scopes.md),
  [SPEC-0004](spec-0004-branch-placeholder.md).

## 7. Change history

- 2026-07-07 — Override gained `${branch}` placeholder support.
- 2025-10-08 — Introduced global ignored-words preprocessing.
- 2025-10-01 — Initial acronym-from-project-name derivation.
