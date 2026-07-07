package com.github.lucsartes.intellijprojectidentifierplugin.adapters.intellij

import com.github.lucsartes.intellijprojectidentifierplugin.core.BranchChangeDetector
import com.github.lucsartes.intellijprojectidentifierplugin.core.TemplateResolver
import com.github.lucsartes.intellijprojectidentifierplugin.ports.BranchProvider
import com.github.lucsartes.intellijprojectidentifierplugin.ports.SettingsPort
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project

/**
 * Manages branch-change watching for a project. Watching is active only while the identifier override
 * references the `${branch}` placeholder, so projects that don't use it pay no cost. When the branch
 * actually changes, it triggers a watermark refresh via [WatermarkPipelineService].
 */
@Service(Service.Level.PROJECT)
class BranchWatchService(private val project: Project) : Disposable {

    private val log = Logger.getInstance(BranchWatchService::class.java)
    private val templateResolver = TemplateResolver()

    private var handle: AutoCloseable? = null

    /**
     * Re-evaluates whether branch watching is needed. Call on startup and whenever settings change.
     * Idempotent: starts the watcher when the override gains a `${branch}` placeholder and stops it when removed.
     */
    @Synchronized
    fun sync() {
        val override = project.getService(SettingsPort::class.java).load().identifierOverride
        val needed = override != null && templateResolver.usesPlaceholder(override, TemplateResolver.BRANCH)
        when {
            needed && handle == null -> start()
            !needed && handle != null -> stop()
        }
    }

    private fun start() {
        val branchProvider = project.getService(BranchProvider::class.java)
        val detector = BranchChangeDetector(branchProvider.currentBranch())
        handle = branchProvider.addChangeListener {
            val now = branchProvider.currentBranch()
            if (detector.onChange(now)) {
                log.info("Branch changed to '$now' for '${project.name}', refreshing watermark")
                project.getService(WatermarkPipelineService::class.java).rerun()
            }
        }
        log.info("Started branch watching for '${project.name}'")
    }

    private fun stop() {
        runCatching { handle?.close() }.onFailure { t -> log.warn("Failed to stop branch watching", t) }
        handle = null
        log.info("Stopped branch watching for '${project.name}'")
    }

    @Synchronized
    override fun dispose() {
        stop()
    }
}
