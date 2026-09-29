# ADR-0005: Branch placeholder — `${name}` templates and hybrid branch detection

* **Status**: Accepted
* **Decided**: 2026-07-07
* **Last updated**: 2026-09-24

## 1. Context

[SPEC-0004](../specs/spec-0004-branch-placeholder.md) requires the identifier override to embed the current Git
branch through `${branch}`, and the watermark to refresh when the branch changes. Three questions follow: how to
represent and resolve placeholders, how to detect the branch and its changes, and how to do both without breaking
the hexagonal boundaries ([ADR-0002](adr-0002-hexagonal-architecture.md)) or adding a hard plugin dependency.

## 2. Decision drivers

* **Purity** — substitution is pure and unit-testable, with no IDE or VCS types in the core.
* **Extensibility** — future placeholders must not need a redesign.
* **Graceful degradation** — non-Git projects, detached HEAD and a disabled Git plugin must all keep working.
* **Low overhead** — branch watching costs nothing for projects that don't use it.
* **Dependency posture** — the only hard dependency stays `com.intellij.modules.platform`.

## 3. Considered options

**Placeholder syntax**

* **A — `${name}` tokens resolved by a pure, map-driven resolver.** Familiar shell-like syntax, unlikely to
  clash with real text; a new placeholder is a new map entry; unknown tokens stay as typed.
* **B — An "append branch" checkbox.** No control over placement, and every future value needs its own control.
* **C — A full template language.** Overkill: large surface, more ways to fail.

**Branch detection**

* **(i) Read and poll `.git/HEAD`.** No dependency and works everywhere, but polling lags and costs a timer, and
  worktree/submodule layouts must be parsed by hand.
* **(ii) Listen to the bundled Git plugin (Git4Idea).** Instant, nearly free, and already correct for detached
  HEAD, worktrees, submodules and multi-root projects, but only available when that plugin is enabled.

## 4. Decision

Adopt **option A** for the syntax, and **both (i) and (ii)** for detection, behind a single `BranchProvider`
port that uses only JDK types.

* **Git4Idea is an optional dependency.** The event-driven provider is registered only from an optional plugin
  descriptor, loaded when the Git plugin is present, where it overrides the default service. When Git4Idea is
  missing or disabled, the plugin still loads and the `.git/HEAD` provider (the default registration) takes
  over, polling at a slow interval. The feature works either way; only the refresh latency differs.
* **The pure pieces live in the core:** placeholder resolution (a null value becomes an empty string, nothing is
  trimmed, unknown placeholders are kept), `.git/HEAD` and `gitdir:` parsing, and a "has the branch really
  changed?" check. Change events are coarse (Git fires on many kinds of activity), so this check is what keeps
  refreshes to real branch switches.
* **Pay only when used.** Watching starts only while the override contains `${branch}`. It is re-evaluated on
  startup and on every project-settings change. The branch is only looked up when the text references it.
* **Refresh via the shared pipeline.** A branch change is one more trigger of the serialized refresh pipeline
  ([ADR-0006](adr-0006-serialized-refresh-pipeline.md)).

## 5. Consequences

* **Positive** — adding a placeholder takes one map entry. Refresh is instant with Git4Idea and still works
  without it. No new hard dependency.
* **Negative** — two provider implementations to maintain, which can differ in edge cases (for example a
  project whose root is not itself a repository is only resolved by the Git4Idea provider). Adding a new
  refresh trigger is also what exposed the need for a serialized pipeline (ADR-0006).

## 6. Code pointers

* `ports/BranchProvider.kt` — the port.
* `adapters/intellij/GitBranchProvider.kt`, `FileSystemBranchProvider.kt` — the two providers.
* `src/main/resources/META-INF/git-integration.xml` — the optional descriptor.

## 7. Related

* **Serves**: [SPEC-0004 — Branch placeholder](../specs/spec-0004-branch-placeholder.md), and the placeholder
  part of [SPEC-0002 §3.3](../specs/spec-0002-identifier-derivation.md).
* **Related ADRs**: [ADR-0002](adr-0002-hexagonal-architecture.md) (boundaries),
  [ADR-0003](adr-0003-settings-implementation.md) (settings changes re-evaluate watching),
  [ADR-0006](adr-0006-serialized-refresh-pipeline.md) (the refresh pipeline).
