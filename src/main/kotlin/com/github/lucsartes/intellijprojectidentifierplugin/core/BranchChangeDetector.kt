package com.github.lucsartes.intellijprojectidentifierplugin.core

/**
 * Tracks the last-seen branch and decides whether a change warrants a watermark refresh.
 *
 * Pure domain logic with no IDE dependencies. Thread-safe because branch-change notifications
 * may arrive from different threads (VCS message-bus events, background pollers).
 */
class BranchChangeDetector(seed: String?) {

    private var last: String? = seed

    /**
     * Records [current] and returns true only if it differs from the previously seen value,
     * so callers can skip redundant refreshes when the branch is unchanged.
     */
    @Synchronized
    fun onChange(current: String?): Boolean {
        if (current == last) return false
        last = current
        return true
    }
}
