package com.github.lucsartes.intellijprojectidentifierplugin.adapters.intellij

import com.github.lucsartes.intellijprojectidentifierplugin.core.GitHeadParser
import com.github.lucsartes.intellijprojectidentifierplugin.ports.BranchProvider
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Zero-dependency [BranchProvider] that reads the current branch directly from `.git/HEAD`.
 *
 * Registered as the default in the main plugin descriptor so the `${branch}` feature keeps working
 * even when the bundled Git plugin is disabled or absent. When Git4Idea is available, its
 * event-driven [GitBranchProvider] replaces this implementation (see git-integration.xml).
 *
 * Because there is no VCS event source here, change detection uses a lightweight periodic poll on the
 * platform-managed, project-scoped coroutine scope (cancelled automatically on project close).
 */
class FileSystemBranchProvider(private val project: Project, private val cs: CoroutineScope) : BranchProvider {

    private val log = Logger.getInstance(FileSystemBranchProvider::class.java)

    override fun currentBranch(): String? {
        val gitDir = resolveGitDir() ?: return null
        val head = gitDir.resolve("HEAD")
        if (!Files.isRegularFile(head)) return null
        return runCatching { GitHeadParser.branchFromHead(Files.readString(head)) }
            .onFailure { t -> log.debug("Failed to read HEAD at '$head'", t) }
            .getOrNull()
    }

    override fun addChangeListener(onChange: () -> Unit): AutoCloseable {
        val job = cs.launch {
            var last = currentBranch()
            while (isActive) {
                delay(POLL_INTERVAL_MS)
                val now = currentBranch()
                if (now != last) {
                    last = now
                    onChange()
                }
            }
        }
        return AutoCloseable { job.cancel() }
    }

    /**
     * Resolves the directory that holds HEAD: the `.git` directory for a normal clone, or the
     * per-worktree/submodule git-dir that a `.git` file points to via `gitdir: <path>`.
     */
    private fun resolveGitDir(): Path? {
        val base = project.basePath?.let { Paths.get(it) } ?: return null
        val dotGit = base.resolve(".git")
        return when {
            Files.isDirectory(dotGit) -> dotGit
            Files.isRegularFile(dotGit) -> runCatching {
                val pointer = GitHeadParser.gitDirFromDotGitFile(Files.readString(dotGit)) ?: return@runCatching null
                val path = Paths.get(pointer)
                (if (path.isAbsolute) path else base.resolve(path)).normalize()
            }.getOrNull()
            else -> null
        }
    }

    companion object {
        private const val POLL_INTERVAL_MS = 5_000L
    }
}
