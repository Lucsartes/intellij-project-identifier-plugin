# ADR-0006: Serialized refresh pipeline — one thread, latest run wins

* **Status**: Accepted
* **Decided**: 2026-07-07 (introduced with the branch placeholder; recorded as its own ADR on 2026-09-24)
* **Last updated**: 2026-09-24

## 1. Context

Refreshing a watermark means: derive the text, render the image, write it to disk (deleting the project's
previous image, see [ADR-0001](adr-0001-dynamic-image-generation.md)), then point the IDE background at the new
file. Several independent triggers can start a refresh at almost the same time: project opening, a per-project
settings change, a global settings change affecting every open project, and branch changes
([ADR-0005](adr-0005-branch-placeholder-implementation.md)).

When runs overlap, one run's cleanup can delete the file another run is about to apply, leaving the background
pointing at a missing image, or an older run can finish last and show stale text. The first one was a real bug,
fixed in 0.0.5 by this decision.

## 2. Decision drivers

* **Correctness** — the applied background always shows the newest settings and branch, and never points at a
  deleted file.
* **Responsiveness** — rendering and file I/O stay off the UI thread, and callers never block.
* **Robustness** — a failed run must never break the IDE or later runs.
* **Simplicity** — no locking scattered across callers.

## 3. Considered options

* **A — Run on whichever thread fired the trigger.** Simple, but unsafe: runs interleave (the bug described
  above).
* **B — A lock around the whole run.** Stops interleaving, but callers block, and the order in which waiting
  runs acquire the lock isn't guaranteed, so an older run can still apply last.
* **C — Debounce triggers with a timer.** Merges bursts, but adds latency to every refresh and still needs
  serialization underneath.
* **D — A single dedicated worker thread plus a generation counter.** Every trigger bumps the counter and
  queues a run. Runs execute one at a time, and a run whose generation is no longer the latest is dropped:
  checked before rendering, after rendering, and again on the UI thread just before applying.

## 4. Decision

Adopt **option D**, owned by one project-level pipeline service that every trigger calls.

* **One worker per project.** A single-threaded executor on a dedicated virtual thread (the work is I/O-bound),
  never a shared pool. Cleanup and write from two runs therefore can't interleave.
* **Latest run wins.** The generation checks of option D drop superseded runs silently.
* **Apply on the UI thread, in any modality.** The IDE background is set on the UI thread, and the call is
  allowed to run while a modal dialog is open, so that clicking Apply in Settings refreshes the watermark
  without waiting for the dialog to close (together with the cache priming in ADR-0001).
* **Fail soft.** Each run catches and logs its own failure, and the next trigger simply tries again.
* **Lifecycle.** The executor is shut down with the project. Triggers that arrive while the project is closing
  are ignored, and the settings subscriptions are tied to the service's lifetime.

## 5. Consequences

* **Positive** — any number of triggers can fire concurrently without corrupting the background. New triggers
  are one call to the service. No locks in callers.
* **Negative** — a burst of triggers may still render more than once. The generation checks drop stale results
  but don't coalesce work. That is acceptable, because a render is cheap.

## 6. Code pointers

* `adapters/intellij/WatermarkPipelineService.kt` — the pipeline.
* `adapters/intellij/ProjectStartupActivity.kt` — wires the triggers.

## 7. Related

* **Serves**: [SPEC-0001 — Project watermark](../specs/spec-0001-project-watermark.md) (§3.1, when the
  watermark is regenerated).
* **Related ADRs**: [ADR-0001](adr-0001-dynamic-image-generation.md) (the image and file lifecycle),
  [ADR-0003](adr-0003-settings-implementation.md) (settings triggers),
  [ADR-0005](adr-0005-branch-placeholder-implementation.md) (branch trigger).
