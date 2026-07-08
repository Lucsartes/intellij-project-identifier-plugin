# ADR-0005: Branch placeholder implementation

> Technical decision. Serves [SPEC-0004 — Branch placeholder](../specs/spec-0004-branch-placeholder.md).
> Living document — keep in sync with the code (see [`docs/README.md`](../README.md)).

* **Status**: Accepted
* **Last updated**: 2026-07-08 (originally decided 2026-07-07)
* **Authors**: Project maintainers

## 1. Context

[SPEC-0004](../specs/spec-0004-branch-placeholder.md) requires the identifier override to embed the current Git
branch via `${branch}` and to refresh the watermark when the branch changes — without violating the hexagonal
boundaries ([ADR-0002](adr-0002-hexagonal-architecture.md)), without adding a *hard* dependency
([ADR-0001](adr-0001-dynamic-image-generation.md)), and without duplicating IDE responsibilities
([ADR-0003](adr-0003-settings-implementation.md)). The technical questions: how to represent/resolve the
placeholder, how to detect the branch and its changes, and how to add a new refresh trigger safely.

## 2. Decision drivers

* **Purity** — substitution must be pure and unit-testable; no IDE/VCS types in `core`.
* **Extensibility** — future placeholders (`${project}`, `${sha}`) must not require redesign.
* **Graceful degradation** — non-Git projects, detached HEAD, and a disabled Git plugin must all work.
* **Low overhead** — branch watching must be cheap and only active when actually needed.
* **Correctness** — a new refresh trigger must not corrupt the applied background under concurrency.
* **Minimal dependency posture** — no new *hard* plugin dependency.

## 3. Considered options

**Placeholder syntax & substitution**
* **A — `${name}` token, substituted by a pure resolver.** Shell-like, collision-safe, map-driven so new
  tokens are one-line additions; unknown tokens preserved verbatim.
* **B — a dedicated "append branch" checkbox.** Rigid: no placement control; each future value needs its own
  control.
* **C — a full templating language.** Overkill; large surface, more failure modes.

**Branch detection & change notification**
* **(i) Poll `.git/HEAD`.** Zero dependencies, works headless, but adds a permanent per-project timer, lags up
  to the poll interval, and requires hand-rolling detached-HEAD / worktree / submodule parsing.
* **(ii) Event-driven via the bundled Git plugin (Git4Idea).** Subscribe to `GitRepository.GIT_REPO_CHANGE` and
  read `currentBranchName`. Instant, near-zero cost, and correctly handles detached HEAD, worktrees, submodules,
  and multi-root repos. Requires the bundled Git plugin.

## 4. Decision

Adopt **Option A** (`${branch}` syntax) with a **hybrid** detection strategy behind a single pure port.

* **Port.** `ports/BranchProvider` (`currentBranch(): String?` + `addChangeListener(...): AutoCloseable`) keeps
  the core and pipeline free of VCS types.
* **Primary adapter.** `GitBranchProvider` (event-driven, Git4Idea) is registered **only** via the optional
  descriptor `git-integration.xml`, gated by
  `<depends optional="true" config-file="git-integration.xml">Git4Idea</depends>`, and marked `overrides="true"`
  so it replaces the default when Git is present.
* **Fallback adapter.** `FileSystemBranchProvider` (reads `.git/HEAD`, polls on the platform-managed
  project-scoped coroutine scope) is the **default** registered in the main descriptor, so the feature works
  dependency-free when the Git plugin is disabled or absent.
* **Pure helpers in core.** `TemplateResolver` performs `${name}` substitution (empty string for a null value,
  unknown placeholders preserved, no trimming). `GitHeadParser` parses `.git/HEAD` (symbolic ref → branch;
  detached HEAD → null) and `gitdir:` pointers for worktrees/submodules. `BranchChangeDetector` implements the
  compare-and-skip logic so only an *actual* branch change triggers a refresh.
* **Cost gating.** `BranchWatchService` starts branch watching only while the override actually contains
  `${branch}` (checked via `TemplateResolver.usesPlaceholder`), and the pipeline resolves the branch value only
  when the text references it — so projects that don't use the placeholder pay nothing.
* **No-branch behavior.** `${branch}` → empty string, surrounding text preserved (matches
  [SPEC-0004](../specs/spec-0004-branch-placeholder.md)).
* **UI.** A `HelpTooltip` on the override field documents the placeholder (two i18n keys, see
  [ADR-0004](adr-0004-internationalization-implementation.md)).

## 5. Consequences

* **Positive**:
  * The override is a tiny, extensible template; more placeholders are a one-line map addition.
  * Instant, near-zero-cost refresh when Git4Idea is available; graceful fallback otherwise.
  * The plugin's only *hard* dependency remains `com.intellij.modules.platform`.
* **Negative / Neutral**:
  * A new refresh trigger (branch change) joins the existing settings-change triggers. To keep this correct, the
    render/apply flow was extracted into `WatermarkPipelineService` and **hardened**: runs are serialized on a
    single dedicated (virtual) thread, and a monotonic **generation counter** ensures only the latest run
    applies. This also fixed a pre-existing race where two overlapping runs could leave the background pointing
    at an image a later run's cleanup had already deleted. `WatermarkStore` additionally isolates each project's
    files in its own subdirectory so cleanup never touches another project's watermark.
  * Adds an *optional* runtime dependency on the bundled Git plugin (see
    [ADR-0001](adr-0001-dynamic-image-generation.md), amended).

## 6. Reflected in code

- `src/main/kotlin/.../core/TemplateResolver.kt` — pure `${name}` substitution + `usesPlaceholder`.
- `src/main/kotlin/.../core/BranchChangeDetector.kt` — compare-and-skip (thread-safe).
- `src/main/kotlin/.../core/GitHeadParser.kt` — `.git/HEAD` and `gitdir:` parsing.
- `src/main/kotlin/.../ports/BranchProvider.kt` — the pure branch port.
- `src/main/kotlin/.../adapters/intellij/GitBranchProvider.kt` — Git4Idea event-driven provider.
- `src/main/kotlin/.../adapters/intellij/FileSystemBranchProvider.kt` — polling fallback provider.
- `src/main/kotlin/.../adapters/intellij/BranchWatchService.kt` — starts/stops watching by `${branch}` presence.
- `src/main/kotlin/.../adapters/intellij/WatermarkPipelineService.kt` — serialized pipeline + generation
  counter.
- `src/main/kotlin/.../core/WatermarkStore.kt` — per-project file isolation + length-bounded names.
- `src/main/resources/META-INF/plugin.xml` (default `FileSystemBranchProvider`, optional Git4Idea `<depends>`)
  and `src/main/resources/META-INF/git-integration.xml` (`GitBranchProvider`, `overrides="true"`).
- Tests: `src/test/kotlin/.../core/TemplateResolverTest.kt`, `.../core/BranchChangeDetectorTest.kt`,
  `.../core/GitHeadParserTest.kt`, `.../core/WatermarkStoreTest.kt`.

## 7. Related documents

- **Serves**: [SPEC-0004 — Branch placeholder](../specs/spec-0004-branch-placeholder.md); also partially
  [SPEC-0002 — Identifier derivation](../specs/spec-0002-identifier-derivation.md) — the override's `${branch}`
  support relies on `TemplateResolver`, which this ADR introduces.
- **Related ADRs**: [ADR-0001](adr-0001-dynamic-image-generation.md) (dependency posture — amended),
  [ADR-0002](adr-0002-hexagonal-architecture.md) (hexagonal boundaries),
  [ADR-0003](adr-0003-settings-implementation.md) (override semantics),
  [ADR-0004](adr-0004-internationalization-implementation.md) (two new i18n keys).
