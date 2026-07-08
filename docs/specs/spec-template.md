# SPEC-NNNN: [Short product-facing title]

> **What is a spec?** A spec captures a **product / behavioral decision** — *what* the plugin does and *why* —
> described from the user's point of view. Specs deliberately avoid technical mechanics (classes, libraries,
> threading, storage formats); those live in ADRs under [`../adrs/`](../adrs/). Rule of thumb: if a sentence
> only makes sense to someone reading the source code, it belongs in an ADR, not here.
>
> **Living document.** This spec must always reflect the plugin's *current* behavior. When you change
> user-facing behavior in code, update this spec in the same change; when you change this spec, change the code
> so it matches. See [`.claude/CLAUDE.md`](../../.claude/CLAUDE.md) → "Docs & code move together".

* **Status**: [Draft | Accepted | Superseded by [SPEC-NNNN](spec-NNNN-...md)]
* **Last updated**: [YYYY-MM-DD]
* **Owners**: [name(s)]

## 1. Summary

[One short paragraph, in plain language a user would understand: what behavior or feature this spec describes.]

## 2. Context & motivation

[The user problem or need this addresses. Why does the behavior exist? Who benefits, and when? Keep it
non-technical — no IDE APIs, no class names.]

## 3. Behavior

[The heart of the spec: the concrete, observable rules the user can rely on. Every claim here should be
verifiable by *using* the plugin, without reading source code. Cover:
- The inputs / triggers the user provides.
- What the plugin produces or changes as a result.
- Defaults.
- Edge cases and exactly how each behaves.
Use short lists and worked examples.]

## 4. Non-goals / out of scope

[What this deliberately does *not* do, and where that responsibility lives instead — e.g. delegated to the IDE,
or covered by another spec.]

## 5. Reflected in code

[The living-doc anchor. Link the source files and tests that implement this behavior so a reader can jump
between doc and code. Update these links when code moves.

- `src/main/kotlin/.../Something.kt` — what it contributes.
- Tests: `src/test/kotlin/.../SomethingTest.kt`.]

## 6. Related decisions

[- **Realized by**: [ADR-NNNN — ...](../adrs/adr-NNNN-...md)
- **See also**: [SPEC-NNNN — ...](spec-NNNN-...md)]

## 7. Change history

[Optional. Notable behavior changes over time, newest first, each with a date.]

---

### File naming

`spec-NNNN-short-title-in-kebab-case.md`

* **`NNNN`**: zero-padded sequential number (`0001`, `0002`, …), so specs sort and reference cleanly.
* **`short-title-in-kebab-case`**: a brief, lowercase, hyphen-separated version of the title.
