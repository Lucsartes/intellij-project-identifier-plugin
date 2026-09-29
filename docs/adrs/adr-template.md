# ADR-NNNN: <Short technical title, ideally naming the decision>

* **Status**: Proposed | Accepted | Superseded by [ADR-NNNN](adr-NNNN-short-title.md)
* **Decided**: YYYY-MM-DD
* **Last updated**: YYYY-MM-DD

<!--
How to write an ADR (delete this comment when you use the template):

An ADR records ONE technical decision worth explaining: the problem, the options that were weighed, the
choice, and what it costs. It is written for a future maintainer who asks "why is it built this way?".

- Explain the decision, not the code. No class-by-class walkthrough, no line-level detail, no values that
  are tuned in code (timeouts, sizes, defaults): the code is the source of truth for those.
- Name a class or API only when the decision is about it, e.g. "only X may touch the internal API".
- It should change only when the decision changes:
  * a small evolution: add a dated entry under "Amendments";
  * a reversal: write a new ADR, set this one to "Superseded by", and link both ways.
- The user-visible behavior it serves is described in a spec (../specs/); link it rather than repeating it.
-->

## 1. Context

<The technical problem and the forces at play, stated neutrally. Link the spec that needs it.>

## 2. Decision drivers

* **<Driver>** — <why it matters here>.

## 3. Considered options

* **A — <name>.** <One or two sentences.> Pros: … Cons: …
* **B — <name>.** …

## 4. Decision

<The chosen option and why it wins against the drivers. Include the rules that follow from it, such as
boundaries that must not be crossed or invariants the code must keep.>

## 5. Consequences

* **Positive** — …
* **Negative** — … (and how it is mitigated)

## 6. Code pointers

<One to three places to start reading: packages or the classes the decision is about. They are signposts, not
an inventory, so don't list every file and don't describe what each one does.>

## 7. Related

* **Serves**: [SPEC-NNNN — …](../specs/spec-NNNN-short-title.md)
* **Related ADRs**: [ADR-NNNN — …](adr-NNNN-short-title.md)

## Amendments

<Optional. Dated notes for evolutions that refine the decision without reversing it, oldest first.
Example: "**YYYY-MM-DD** — …".>
