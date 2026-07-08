package com.github.lucsartes.intellijprojectidentifierplugin.adapters.intellij

import com.github.lucsartes.intellijprojectidentifierplugin.core.IdentifierGenerator
import com.github.lucsartes.intellijprojectidentifierplugin.core.ImageRenderer
import com.github.lucsartes.intellijprojectidentifierplugin.core.TemplateResolver
import com.github.lucsartes.intellijprojectidentifierplugin.core.WatermarkStore
import com.github.lucsartes.intellijprojectidentifierplugin.ports.ApplicationSettingsPort
import com.github.lucsartes.intellijprojectidentifierplugin.ports.BackgroundImagePort
import com.github.lucsartes.intellijprojectidentifierplugin.ports.BranchProvider
import com.github.lucsartes.intellijprojectidentifierplugin.ports.ProjectSettingsPort
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicLong

/**
 * Owns the watermark pipeline: derive the identifier text (resolving dynamic placeholders such as
 * `${branch}`), render the PNG, persist it, and apply it as the editor/empty-frame background.
 *
 * Reruns are serialized on a single dedicated thread so file cleanup and writes from concurrent
 * triggers (startup, settings changes, branch changes) never interleave, and a monotonic generation
 * counter guarantees only the latest run applies — preventing an earlier run's background from
 * pointing at an image a later run's cleanup already deleted.
 */
@Service(Service.Level.PROJECT)
class WatermarkPipelineService(private val project: Project) : Disposable {

    private val log = Logger.getInstance(WatermarkPipelineService::class.java)
    private val templateResolver = TemplateResolver()
    private val identifierGenerator = IdentifierGenerator()
    private val imageRenderer = ImageRenderer()
    private val watermarkStore = WatermarkStore(Paths.get(PathManager.getSystemPath(), PLUGIN_DIR_ID, "watermarks"))

    // Single-threaded: cleanup + write from different runs cannot interleave. Virtual thread: work is I/O-bound.
    private val executor: ExecutorService = Executors.newSingleThreadExecutor(
        Thread.ofVirtual().name("project-identifier-pipeline-", 0).factory()
    )

    // Only the latest run is allowed to apply; superseded runs are dropped.
    private val generation = AtomicLong(0)

    /** Recomputes and applies the watermark. Safe to call from any thread and from multiple triggers. */
    fun rerun() {
        val gen = generation.incrementAndGet()
        if (executor.isShutdown) return // project is closing
        try {
            executor.execute {
                if (gen != generation.get()) return@execute // superseded before we started

                val result = runCatching { computeAndPersist() }
                    .onFailure { t -> log.warn("Pipeline failed for '${project.name}'", t) }
                    .getOrNull() ?: return@execute

                if (gen != generation.get()) return@execute // a newer run has taken over

                ApplicationManager.getApplication().invokeLater({
                    if (gen != generation.get()) return@invokeLater // stale by the time we reach the EDT
                    runCatching {
                        project.getService(BackgroundImagePort::class.java).setBackgroundImage(result.imagePath)
                        log.info("Applied watermark '${result.text}' for '${project.name}' at '${result.imagePath}'")
                    }.onFailure { t -> log.warn("Failed to apply background image for '${project.name}'", t) }
                }, ModalityState.any())
            }
        } catch (e: RejectedExecutionException) {
            // The executor was shut down between the check above and here (project closing); nothing to do.
            log.debug("Pipeline rerun ignored; project is closing", e)
        }
    }

    private fun computeAndPersist(): PipelineResult {
        val settings = project.getService(ProjectSettingsPort::class.java).load()
        val appSettings = ApplicationManager.getApplication().getService(ApplicationSettingsPort::class.java).load()

        val base = settings.identifierOverride
            ?: identifierGenerator.generate(project.name, appSettings.ignoredWords.toSet())

        // Only pay the cost of resolving the branch when the text actually references it.
        val branch = if (templateResolver.usesPlaceholder(base, TemplateResolver.BRANCH)) {
            project.getService(BranchProvider::class.java).currentBranch()
        } else {
            null
        }
        val text = templateResolver.resolve(base, mapOf(TemplateResolver.BRANCH to branch))
        log.info("Resolved watermark text '$text' for '${project.name}' (branch=$branch)")

        val imageBytes = imageRenderer.renderPng(
            text,
            settings.fontFamily,
            settings.fontSizePx,
            settings.textColorArgb,
        )

        // Each project's files are isolated in their own subdirectory, so cleanup never touches another project.
        val imagePath = watermarkStore.write(project.locationHash, project.name, text, imageBytes)
        log.info("Wrote watermark image to '$imagePath' (${imageBytes.size} bytes)")

        return PipelineResult(imagePath, text)
    }

    override fun dispose() {
        executor.shutdownNow()
    }

    private data class PipelineResult(val imagePath: Path, val text: String)

    companion object {
        private const val PLUGIN_DIR_ID = "com.github.lucsartes.intellijprojectidentifierplugin"
    }
}
