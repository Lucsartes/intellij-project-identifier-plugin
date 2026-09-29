# Project documentation

This folder holds the plugin's design documentation, split into two kinds of document:

| Folder              | Captures                        | Answers                                  | Written for       |
|---------------------|---------------------------------|------------------------------------------|-------------------|
| [`specs/`](specs/)  | **Product / behavioral** rules  | *What* does the plugin do, and *why*?    | A user            |
| [`adrs/`](adrs/)    | **Technical** decisions         | *How* is it built, and *why that way*?   | A maintainer      |

A **spec** describes behavior a user can observe: features, rules, defaults, settings, edge cases. You never
need the source code to understand it, and it names no classes, libraries or IDE APIs.

An **ADR** (Architecture Decision Record) records one technical decision that is worth explaining:
architecture, IDE APIs, dependencies, concurrency, trade-offs. It explains the choice, not the code.

A spec is **realized by** one or more ADRs, and an ADR **serves** one or more specs. They link to each other.

## Keeping docs and code aligned

Both kinds of document are **high level on purpose**, so that most code changes don't touch them. The rule:

> **A doc must never contradict the code.** When they disagree, fix whichever one is wrong in the same change.

In practice:

* **User-visible behavior changes** (new feature, changed rule or default): update the spec, and add a
  `CHANGELOG.md` entry.
* **A technical decision changes**: amend the ADR with a dated entry, or write a new ADR that supersedes it
  when the decision is reversed.
* **Refactors, renames, and bug fixes that restore documented behavior** need no doc change.
* **When you work in an area an ADR covers**, re-read that ADR (its *Code pointers* section helps you find
  it) and check it still holds.

Specs keep no change history: `CHANGELOG.md` and git record what changed and when. This rule is repeated in
[`.claude/CLAUDE.md`](../.claude/CLAUDE.md) so automated assistants follow it.

## Index

### Specs
- [SPEC-0001 — Project watermark](specs/spec-0001-project-watermark.md) — the core feature: a per-project background watermark.
- [SPEC-0002 — Identifier derivation](specs/spec-0002-identifier-derivation.md) — which text the watermark shows.
- [SPEC-0003 — Settings & scopes](specs/spec-0003-settings-and-scopes.md) — what users can configure, and where.
- [SPEC-0004 — Branch placeholder](specs/spec-0004-branch-placeholder.md) — the `${branch}` dynamic placeholder.
- [SPEC-0005 — Internationalization](specs/spec-0005-internationalization.md) — the user interface language.

### ADRs
- [ADR-0001 — Dynamic image generation](adrs/adr-0001-dynamic-image-generation.md) — render a PNG and set it as the IDE background image.
- [ADR-0002 — Hexagonal architecture](adrs/adr-0002-hexagonal-architecture.md) — core / ports / adapters separation.
- [ADR-0003 — Settings implementation](adrs/adr-0003-settings-implementation.md) — plugin/IDE boundary, scopes, persistence, refresh.
- [ADR-0004 — Internationalization implementation](adrs/adr-0004-internationalization-implementation.md) — IntelliJ resource bundles.
- [ADR-0005 — Branch placeholder implementation](adrs/adr-0005-branch-placeholder-implementation.md) — `${name}` templates + hybrid branch detection.
- [ADR-0006 — Serialized refresh pipeline](adrs/adr-0006-serialized-refresh-pipeline.md) — one thread, latest run wins.

### Writing a new document
- Start from the [spec template](specs/spec-template.md) or the [ADR template](adrs/adr-template.md).
  Their comments explain what belongs in each section.
- Name files `spec-NNNN-short-title.md` / `adr-NNNN-short-title.md`: `NNNN` is the next zero-padded number
  in that folder, and the title is lowercase kebab-case. Spec and ADR numbers are independent of each other.
- Add the document to the index above.
