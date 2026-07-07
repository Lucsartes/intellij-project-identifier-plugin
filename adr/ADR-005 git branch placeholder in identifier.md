# ADR-005: Dynamic Git branch placeholder in the identifier

* **Status**: Accepted
* **Date**: 2026-07-07
* **Authors**: Project maintainers

## 1. Context

The per-project **Identifier override** field (ADR-003 §5.1) accepts static text only. Users who juggle
multiple checkouts of the same project — or who switch branches frequently — want the watermark to show the
**current Git branch** and to update automatically on checkout, so they can tell apart windows at a glance
during task switching.

The override text therefore needs to become a small template: users type something like `XXX - ${branch}`,
and the plugin substitutes the current branch. The branch changes over time, so the watermark must refresh
when it does.

This must be achieved without breaking the plugin's architecture principles: the core stays free of IDE/VCS
types (ADR-002), the plugin keeps a minimal hard-dependency footprint and degrades gracefully (ADR-001), and
we don't duplicate IDE responsibilities (ADR-003).

## 2. Decision Drivers

* **Purity**: Placeholder substitution must be pure and unit-testable; no IDE/VCS types in `core`.
* **Extensibility**: Adding future placeholders (e.g. `${project}`, `${sha}`) must not require redesign.
* **Graceful degradation**: Non-Git projects, detached HEAD, and a disabled Git plugin must not break anything.
* **Low overhead**: Watching for branch changes must be cheap and only active when actually needed.
* **Correctness**: A new refresh trigger must not corrupt the applied background (see §5, the concurrency fix).
* **Minimal dependency posture**: No new *hard* plugin dependency (ADR-001).

## 3. Considered Options

### Placeholder syntax & substitution
* **Option A — `${branch}` token, substituted by an adapter, refreshed on branch change.** Shell-like,
  collision-safe, easy to extend to more tokens. A pure `TemplateResolver` in `core` performs the
  substitution from a `Map<name, value>`; unknown placeholders are left verbatim.
* **Option B — a dedicated "append branch" checkbox.** Simpler UI but rigid: no control over placement or
  surrounding text, and every future dynamic value needs its own control.
* **Option C — a full templating language.** Overkill; large surface, more failure modes.

### Branch detection & change notification
* **(i) Poll `.git/HEAD` every few seconds**, comparing last vs current branch (the original idea). Zero
  plugin dependencies and works headless, but adds a permanent timer per open project and lags up to the
  poll interval. Requires re-implementing detached-HEAD / worktree / submodule parsing by hand.
* **(ii) Event-driven via the bundled Git plugin (Git4Idea)**: subscribe to `GitRepository.GIT_REPO_CHANGE`
  and read `GitRepository.currentBranchName`. Instant, near-zero cost (Git4Idea already watches the repo),
  and correctly handles detached HEAD, worktrees, submodules and multi-root repositories. Requires access
  to the bundled Git plugin.

## 4. Decision

Adopt **Option A** with the `${branch}` syntax, and a **hybrid** branch-detection strategy behind a single
pure port.

* A new pure port `ports/BranchProvider` (`currentBranch(): String?` + `addChangeListener(...): AutoCloseable`)
  keeps `core` and the pipeline free of VCS types.
* `GitBranchProvider` (event-driven, Git4Idea) is the primary implementation and is registered only via an
  **optional** dependency descriptor (`git-integration.xml`, gated by
  `<depends optional="true" config-file="git-integration.xml">Git4Idea</depends>`), overriding the default.
* `FileSystemBranchProvider` (reads `.git/HEAD`, polls) is the **default** registered in the main descriptor,
  so the feature still works — dependency-free — when the Git plugin is disabled or absent.
* A pure `core/TemplateResolver` performs the substitution; `core/BranchChangeDetector` implements the
  compare-and-skip logic so a refresh happens only on an actual branch change.
* Branch watching is started only when the override actually contains `${branch}` (managed by
  `BranchWatchService`), so projects that don't use the placeholder pay no cost.
* **No-branch behavior**: when there is no branch (non-Git project, or detached HEAD), `${branch}` is
  replaced with an **empty string**, with no trimming of the surrounding text. For example
  `"XXX - ${branch} - YYY"` renders as `"XXX -  - YYY"`. This is the most predictable behavior and keeps the
  resolver trivial.
* The UI advertises the placeholder via a `HelpTooltip` on the override field (see ADR-004 for the two new
  i18n keys).

## 5. Consequences

* **Positive**:
    * The override field is now a tiny, extensible template; more placeholders are a one-line map addition.
    * Branch changes refresh the watermark instantly when Git4Idea is available, with essentially no cost.
    * The feature degrades gracefully: empty string when there is no branch; filesystem fallback when the
      Git plugin is off; the plugin's only *hard* dependency remains `com.intellij.modules.platform`.
* **Negative / Neutral**:
    * A new refresh trigger (branch change) is added alongside the existing settings-change triggers. To keep
      this correct, the render/apply pipeline was extracted into `WatermarkPipelineService` and hardened:
      runs are serialized on a single dedicated thread and a generation counter ensures only the latest run
      applies. This fixes a pre-existing race where two overlapping runs could leave the background pointing
      at an image that a later run's cleanup had already deleted.
    * Adds an optional runtime dependency on the bundled Git plugin (see ADR-001, amended).
    * The template semantics slightly change what the override field means (see ADR-003, amended).

## 6. Related Documents

* ADR-001 (dependency posture — amended), ADR-002 (hexagonal boundaries), ADR-003 (override semantics —
  amended), ADR-004 (two new i18n keys), README.md, CHANGELOG.md.
