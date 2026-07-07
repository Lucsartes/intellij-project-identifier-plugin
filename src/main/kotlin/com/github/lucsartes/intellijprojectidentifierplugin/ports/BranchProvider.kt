package com.github.lucsartes.intellijprojectidentifierplugin.ports

/**
 * Port that exposes the current VCS branch and notifies when it may have changed.
 * Pure contract (only JDK types) so the core stays free of IDE/VCS dependencies.
 */
interface BranchProvider {
    /** The current branch name, or null when detached, unavailable, or not a repository. */
    fun currentBranch(): String?

    /**
     * Registers [onChange], invoked (possibly on a background thread) whenever the branch may have
     * changed. Callers should compare [currentBranch] to decide whether to act. Close the returned
     * handle to stop listening.
     */
    fun addChangeListener(onChange: () -> Unit): AutoCloseable
}
