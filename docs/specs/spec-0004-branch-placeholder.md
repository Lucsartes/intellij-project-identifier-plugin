# SPEC-0004: Branch placeholder

* **Status**: Accepted
* **Last updated**: 2026-09-24

## 1. Summary

The per-project identifier override (see [SPEC-0002 §3.3](spec-0002-identifier-derivation.md)) accepts the
placeholder `${branch}`, which is replaced by the project's current Git branch. When the user switches
branches, the watermark updates by itself. This lets someone with several checkouts of the same repository,
or who moves between feature branches, tell windows apart by branch.

## 2. Motivation

A fixed override can't tell apart two windows of the *same* project sitting on *different* branches. Showing
the live branch in the watermark solves that. The watermark has to stay correct as the user checks out other
branches, without them touching any settings.

## 3. Behavior

**Syntax.** Type `${branch}` in the override wherever the branch should appear, for example `XXX - ${branch}`.
The surrounding text stays exactly as written. The name is case-sensitive: `${Branch}` is not recognized and is
shown as typed.

**Substitution.**

- `${branch}` becomes the current branch name, exactly as Git shows it, slashes included (e.g.
  `feature/JIRA-123`).
- In a project with several Git repositories, the branch shown is the one of the repository at the project's
  root.
- When there is **no branch** (the project is not a Git repository, or Git is on a detached HEAD), `${branch}`
  becomes an **empty string**. The surrounding text is kept as-is, not trimmed:
  `XXX - ${branch} - YYY` shows `XXX -  - YYY`.

**Automatic refresh.**

- While the override contains `${branch}`, the watermark is regenerated whenever the branch actually changes.
  Other repository activity (commits, fetches, file changes) doesn't trigger a redraw.
- With the IDE's bundled Git integration enabled, the update is **instant** on checkout. If that integration
  is disabled, the feature still works and the watermark updates **within a few seconds**.
- If the override doesn't use `${branch}`, the plugin doesn't watch the branch at all.

**Discoverability.** A help tooltip on the override field explains the syntax and the empty-string rule.

## 4. Out of scope

- Other placeholders: only `${branch}` exists today.
- The automatic acronym: `${branch}` only works inside the override.
- Branch management: the plugin only reads the current branch. It never creates or switches branches.

## 5. Related

- **Realized by**: [ADR-0005 — Branch placeholder implementation](../adrs/adr-0005-branch-placeholder-implementation.md).
- **See also**: [SPEC-0002 — Identifier derivation](spec-0002-identifier-derivation.md),
  [SPEC-0003 — Settings & scopes](spec-0003-settings-and-scopes.md).
