# SPEC-0002: Identifier derivation

* **Status**: Accepted
* **Last updated**: 2026-09-24

## 1. Summary

The watermark text, called the *identifier*, is normally a short acronym built automatically from the project
name. The user can influence it in two ways: a global list of **ignored words** removed before the acronym is
built, and a per-project **override** that replaces the acronym with custom text (which may contain dynamic
placeholders). This spec defines how the final text is produced.

## 2. Motivation

A full project name is too long to serve as a glanceable marker. Many organizations also prefix project names
with boilerplate (`tr-`, `internal-`, …) that tells projects apart no better than nothing. A short acronym of
the *meaningful* words is compact and recognizable. Some projects need a hand-picked marker instead, so an
override is offered.

## 3. Behavior

### 3.1 Automatic derivation (the default)

1. The project name is split into **words**: runs of letters or digits. Everything else (spaces, hyphens,
   underscores, dots, punctuation) separates words. Letters and digits from any alphabet count, not only ASCII.
2. Words matching an **ignored word** are dropped (see §3.2).
3. The identifier is the **first character of each remaining word, uppercased**, in order.

| Project name              | Identifier |
|---------------------------|------------|
| `My Awesome Project`      | `MAP`      |
| `  many   spaces  here `  | `MSH`      |
| `foo-bar_baz`             | `FBB`      |
| `shop-api`                | `SA`       |
| `2024-report`             | `2R`       |

If the project name is blank, or every word is ignored, the identifier is empty and no watermark text is shown.

### 3.2 Ignored words (global)

- A comma-separated list of words removed from every project name before the acronym is built.
- Matching is **case-insensitive** and on **whole words only**: `tr` removes the word `TR` but not `trade`.
- Example: with ignored words `tr, tre`, the project `tr-my-project` gives `MP` instead of `TMP`.
- Default: empty (nothing ignored).
- The list applies to all projects (see [SPEC-0003 §3.2](spec-0003-settings-and-scopes.md)).

### 3.3 Per-project override

- If a project has an **identifier override**, that text is used as-is instead of the acronym. Ignored words
  and the acronym rule do not apply to it.
- The override may contain placeholders written `${name}`, filled in automatically. The only supported
  placeholder today is `${branch}` (see [SPEC-0004](spec-0004-branch-placeholder.md)). Any other `${…}` text
  is shown unchanged.
- An empty or blank override means "no override", and the acronym is used.
- The override is set per project (see [SPEC-0003 §3.1](spec-0003-settings-and-scopes.md)).

### 3.4 Rendering

The resolved text is drawn with the project's font, size and color settings, whose defaults are listed in
[SPEC-0003 §3.1](spec-0003-settings-and-scopes.md). This spec governs only the text *content*.

## 4. Out of scope

- Where and how faintly the text appears: that is the IDE's background image display (see
  [SPEC-0001](spec-0001-project-watermark.md)).
- Stemming, fuzzy or partial matching of ignored words.
- Choosing a different derivation rule (for example keeping short names whole). There is one rule, plus the
  override.

## 5. Related

- **Realized by**: [ADR-0002 — Hexagonal architecture](../adrs/adr-0002-hexagonal-architecture.md) (the
  derivation is pure, IDE-independent logic),
  [ADR-0005 — Branch placeholder implementation](../adrs/adr-0005-branch-placeholder-implementation.md)
  (placeholder syntax).
- **See also**: [SPEC-0001](spec-0001-project-watermark.md), [SPEC-0003](spec-0003-settings-and-scopes.md),
  [SPEC-0004](spec-0004-branch-placeholder.md).
