# ADR-NNNN: [Short technical title]

> **What is an ADR?** An Architecture Decision Record captures a **technical decision** — *how* the plugin is
> built and *why that approach*. ADRs cover architecture, libraries, IDE APIs, storage formats, algorithms,
> concurrency, and trade-offs. The *product* behavior an ADR serves is described in a spec under
> [`../specs/`](../specs/); keep the user-facing "what/why" there and the "how" here.
>
> **Living document.** This ADR must reflect the code **as it stands today**, not just the decision as first
> made. When you change the technical approach in code, update the ADR in the same change; when you change an
> ADR, change the code to match. For a small evolution, amend the ADR with a dated note; for a reversal, write a
> new ADR that supersedes this one. See [`.claude/CLAUDE.md`](../../.claude/CLAUDE.md).

* **Status**: [Proposed | Accepted | Superseded by [ADR-NNNN](adr-NNNN-...md)]
* **Last updated**: [YYYY-MM-DD]
* **Authors**: [name(s)]

## 1. Context

[The technical dilemma or problem. What is the driver for this decision? What is the current state and what
forces are at play? Frame it neutrally.]

## 2. Decision drivers

[The key technical factors and constraints, e.g.:
* **Maintainability** — …
* **Testability** — …
* **API stability / dependency footprint** — …
* **Performance** — …]

## 3. Considered options

[Each option with a brief description and an honest pros/cons against the drivers.
* **Option A** — description. Pros: … Cons: …
* **Option B** — …
* **Option C** — …]

## 4. Decision

[State the chosen option explicitly and justify it against the drivers.]

## 5. Consequences

[* **Positive** — …
* **Negative** — …
* **Neutral** — changes that simply need to be done.]

## 6. Reflected in code

[The living-doc anchor. Link the source files (and tests) that implement this decision, so the ADR stays tied
to reality. Update these links when code moves.
- `src/main/kotlin/.../Something.kt` — what it implements.
- Tests: `src/test/kotlin/.../SomethingTest.kt`.]

## 7. Related documents

[- **Serves**: [SPEC-NNNN — ...](../specs/spec-NNNN-...md)
- **Related ADRs**: [ADR-NNNN — ...](adr-NNNN-...md)
- External references, if any.]

---

### File naming

`adr-NNNN-short-title-in-kebab-case.md`

* **`NNNN`**: zero-padded sequential number (`0001`, `0002`, …), so ADRs sort and reference cleanly.
* **`short-title-in-kebab-case`**: a brief, lowercase, hyphen-separated version of the title.
