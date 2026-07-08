# Project documentation

This folder holds the plugin's **living documentation**. It is split into two kinds of document:

| Folder        | Captures                          | Answers                              | Audience frame           |
|---------------|-----------------------------------|--------------------------------------|--------------------------|
| [`specs/`](specs/)   | **Product / behavioral** decisions | *What* does the plugin do, and *why*? | The user's point of view |
| [`adrs/`](adrs/)     | **Technical** decisions            | *How* is it built, and *why that way*? | The maintainer's point of view |

A **spec** describes observable behavior: features, user-facing rules, settings semantics, supported
languages, edge cases. Nothing in a spec should require reading source code to understand.

An **ADR** (Architecture Decision Record) describes a technical choice: architecture, libraries, IDE APIs,
storage formats, algorithms, concurrency, trade-offs. One spec is typically **realized by** one or more ADRs;
one ADR **serves** one or more specs. They cross-link each other.

## Living-document rule

Both specs and ADRs must always reflect the plugin **as it is today** — not just as it was first designed.

> **Docs and code move together.** When you change code, update the affected spec/ADR in the *same* change.
> When you change a spec/ADR, change the code so it still matches. A doc that disagrees with the code is a bug.

Each document has a **Reflected in code** section linking to the source files and tests that implement it, so
you can jump from doc to code and back, and notice quickly when they drift apart. This rule is also recorded
in [`.claude/CLAUDE.md`](../.claude/CLAUDE.md) so automated assistants follow it.

## Index

### Specs
- [SPEC-0001 — Project watermark](specs/spec-0001-project-watermark.md) — the core feature: a per-project background watermark.
- [SPEC-0002 — Identifier derivation](specs/spec-0002-identifier-derivation.md) — how watermark text is derived from the project name.
- [SPEC-0003 — Settings & scopes](specs/spec-0003-settings-and-scopes.md) — what users can configure, and where.
- [SPEC-0004 — Branch placeholder](specs/spec-0004-branch-placeholder.md) — the `${branch}` dynamic placeholder.
- [SPEC-0005 — Internationalization](specs/spec-0005-internationalization.md) — multi-language user interface.

### ADRs
- [ADR-0001 — Dynamic image generation](adrs/adr-0001-dynamic-image-generation.md) — render a PNG and set it as the editor background.
- [ADR-0002 — Hexagonal architecture](adrs/adr-0002-hexagonal-architecture.md) — core / ports / adapters separation.
- [ADR-0003 — Settings implementation](adrs/adr-0003-settings-implementation.md) — configurables, persistent services, storage.
- [ADR-0004 — Internationalization implementation](adrs/adr-0004-internationalization-implementation.md) — `DynamicBundle` + resource bundles.
- [ADR-0005 — Branch placeholder implementation](adrs/adr-0005-branch-placeholder-implementation.md) — hybrid branch detection + serialized pipeline.

### Templates
- [Spec template](specs/spec-template.md)
- [ADR template](adrs/adr-template.md)
