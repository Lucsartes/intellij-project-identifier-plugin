package com.github.lucsartes.intellijprojectidentifierplugin.core

/**
 * Pure parser for the parts of a Git repository layout needed to read the current branch,
 * without any Git tooling or IDE dependency. Used by the filesystem-based branch provider.
 */
object GitHeadParser {

    private const val REF_HEADS_PREFIX = "ref: refs/heads/"
    private const val GITDIR_PREFIX = "gitdir:"

    /**
     * Extracts the branch name from the content of a `.git/HEAD` file.
     *
     * Returns the branch for a symbolic ref (`ref: refs/heads/<branch>`), or null when the
     * repository is in a detached-HEAD state (HEAD holds a raw commit hash) or the content is unusable.
     * Branch names containing slashes (e.g. `feature/JIRA-123`) are preserved verbatim.
     */
    fun branchFromHead(headContent: String): String? {
        val firstLine = headContent.lineSequence().firstOrNull()?.trim().orEmpty()
        if (!firstLine.startsWith(REF_HEADS_PREFIX)) return null
        return firstLine.removePrefix(REF_HEADS_PREFIX).trim().ifBlank { null }
    }

    /**
     * For linked worktrees and submodules, `.git` is a regular file of the form `gitdir: <path>`.
     * Returns the referenced git-dir path (which may be relative), or null if the content is not a gitdir pointer.
     */
    fun gitDirFromDotGitFile(content: String): String? {
        val firstLine = content.lineSequence().firstOrNull()?.trim().orEmpty()
        if (!firstLine.startsWith(GITDIR_PREFIX)) return null
        return firstLine.removePrefix(GITDIR_PREFIX).trim().ifBlank { null }
    }
}
