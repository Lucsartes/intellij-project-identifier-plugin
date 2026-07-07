package com.github.lucsartes.intellijprojectidentifierplugin.adapters.intellij

import com.github.lucsartes.intellijprojectidentifierplugin.ports.BranchProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryChangeListener
import git4idea.repo.GitRepositoryManager

/**
 * Event-driven [BranchProvider] backed by the bundled Git plugin (Git4Idea).
 *
 * This class references git4idea types, so it is registered ONLY from the optional
 * git-integration.xml descriptor (loaded when the Git plugin is present and enabled), where it
 * overrides the default [FileSystemBranchProvider]. Git4Idea already correctly handles detached
 * HEAD, worktrees, submodules and multi-root repositories, and fires GIT_REPO_CHANGE on checkout,
 * so we get instant, near-zero-cost branch-change notifications instead of polling.
 */
class GitBranchProvider(private val project: Project) : BranchProvider {

    override fun currentBranch(): String? {
        val manager = GitRepositoryManager.getInstance(project)
        val repository = project.guessProjectDir()?.let { manager.getRepositoryForRootQuick(it) }
            ?: manager.repositories.firstOrNull()
            ?: return null
        // Null when the repository is on a detached HEAD; the pipeline substitutes an empty string.
        return repository.currentBranchName
    }

    override fun addChangeListener(onChange: () -> Unit): AutoCloseable {
        // GIT_REPO_CHANGE is coarse (fires on many git events); the caller debounces by comparing branches.
        val connection = project.messageBus.connect()
        connection.subscribe(GitRepository.GIT_REPO_CHANGE, GitRepositoryChangeListener { onChange() })
        return AutoCloseable { connection.disconnect() }
    }
}
