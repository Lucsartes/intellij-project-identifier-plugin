package com.github.lucsartes.intellijprojectidentifierplugin.adapters.intellij

import com.github.lucsartes.intellijprojectidentifierplugin.core.PluginSettings
import com.github.lucsartes.intellijprojectidentifierplugin.ports.BackgroundImagePort
import com.github.lucsartes.intellijprojectidentifierplugin.ports.IdentifierService
import com.github.lucsartes.intellijprojectidentifierplugin.ports.ImageService
import com.github.lucsartes.intellijprojectidentifierplugin.ports.SettingsPort
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import java.awt.GraphicsEnvironment
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

/**
 * Main entry point: runs on project open and orchestrates the pipeline
 * (generate identifier -> generate image -> save image -> set background).
 * It also listens to settings changes to re-run the pipeline.
 */
class ProjectStartupActivity : ProjectActivity {

    private val log = Logger.getInstance(ProjectStartupActivity::class.java)

    override suspend fun execute(project: Project) {
        log.info("Starting Project Identifier pipeline for project '${project.name}' on startup")
        // Run once on startup in background (compute + persist), then apply on EDT
        rerunPipelineAsync(project)

        // Subscribe to settings changes and rerun the pipeline asynchronously
        val connection = project.messageBus.connect()
        connection.subscribe(
            IntelliJSettingsService.TOPIC,
            object : IntelliJSettingsService.SettingsChangedListener {
                override fun settingsChanged(newSettings: PluginSettings) {
                    log.info("Settings changed for '${project.name}': override=${newSettings.identifierOverride}, fontFamily=${newSettings.fontFamily}, fontSizePx=${newSettings.fontSizePx}, textColorArgb=${newSettings.textColorArgb}")
                    rerunPipelineAsync(project)
                }
            }
        )
        log.info("Subscribed to Project Identifier settings changes for project '${project.name}'")
    }

    private fun rerunPipelineAsync(project: Project) {
        val app = ApplicationManager.getApplication()
        app.executeOnPooledThread {
            val result = runCatching { runPipelineComputeAndPersist(project) }
                .onFailure { t -> log.warn("Pipeline failed in background for '${project.name}'", t) }
                .getOrNull()

            if (result == null) return@executeOnPooledThread
            if (!result.shouldApply) {
                log.info("No image to apply for '${project.name}'")
                return@executeOnPooledThread
            }

            app.invokeLater({
                try {
                    log.info("Pipeline step: apply background via port (EDT)")
                    val backgroundSetter = project.getService(BackgroundImagePort::class.java)
                    backgroundSetter.setBackgroundImage(result.imagePath)
                    log.info("Applied project identifier watermark for '${result.projectName}' at '${result.imagePath}'")
                } catch (t: Throwable) {
                    log.warn("Failed to apply background image on EDT for '${project.name}'", t)
                }
            }, ModalityState.any())
        }
    }

    private data class PipelineResult(
        val imagePath: Path,
        val projectName: String,
        val shouldApply: Boolean,
    )

    private fun runPipelineComputeAndPersist(project: Project): PipelineResult {
        log.info("Pipeline step: load settings for project '${project.name}'")
        val settings = project.getService(SettingsPort::class.java).load()
        log.info("Settings loaded (project-scoped): override=${settings.identifierOverride}, fontFamily=${settings.fontFamily}, fontSizePx=${settings.fontSizePx}, textColorArgb=${settings.textColorArgb}")

        val projectName = project.name
        log.info("Pipeline step: derive identifier (projectName='$projectName', hasOverride=${settings.identifierOverride != null})")
        val identifierService = ApplicationManager.getApplication().getService(IdentifierService::class.java)
        val text = settings.identifierOverride ?: identifierService.generate(projectName)
        log.info("Identifier resolved: '$text'")

        log.info("Pipeline step: render PNG for identifier")
        val imageService = ApplicationManager.getApplication().getService(ImageService::class.java)
        // Prefer a bundled IntelliJ font (JetBrains Mono) when no font is specified.
        // This keeps the core pure while offering a sensible default that aligns with IDE look & feel.
        val effectiveFontFamily: String? = run {
            val requested = settings.fontFamily?.ifBlank { null }
            if (requested != null) return@run requested
            // Only choose JetBrains Mono if the JRE reports it as available
            val available = try {
                GraphicsEnvironment.getLocalGraphicsEnvironment().availableFontFamilyNames.toSet()
            } catch (_: Throwable) {
                emptySet<String>()
            }
            if ("JetBrains Mono" in available) "JetBrains Mono" else null
        }
        val imageBytes = imageService.renderPng(text, effectiveFontFamily, settings.fontSizePx, settings.textColorArgb)
        log.info("Image rendered: ${imageBytes.size} bytes (font='${effectiveFontFamily ?: settings.fontFamily}', colorArgb='${settings.textColorArgb}')")

        log.info("Pipeline step: resolve image path")
        val imagePath = resolveImagePath(project, text)
        log.info("Image path resolved: '$imagePath'")

        // Ensure parent directory exists, cleanup previous files, and write the new file
        log.info("Pipeline step: write image to disk (with cleanup)")
        Files.createDirectories(imagePath.parent)
        // Best-effort cleanup to avoid stale files and force IntelliJ to reload when name changes
        runCatching {
            val safeProject = project.name.replace("[^A-Za-z0-9._-]".toRegex(), "_")
            Files.list(imagePath.parent).use { stream ->
                stream.forEach { p ->
                    val name = p.fileName.toString()
                    val matchesThisProject = name.startsWith("$safeProject-")
                    if (matchesThisProject) {
                        runCatching { Files.deleteIfExists(p) }
                            .onFailure { t -> log.warn("Failed to delete old watermark file '$p'", t) }
                    }
                }
            }
        }.onFailure { t -> log.warn("Failed to cleanup watermark directory '${imagePath.parent}'", t) }

        Files.write(imagePath, imageBytes)
        log.info("Image written to '$imagePath' (${imageBytes.size} bytes)")

        return PipelineResult(imagePath = imagePath, projectName = projectName, shouldApply = true)
    }

    private fun resolveImagePath(project: Project, text: String): Path {
        // Centralized, plugin-specific directory under IDE system path (cache-like storage)
        val safeProject = project.name.replace("[^A-Za-z0-9._-]".toRegex(), "_")
        val safeText = text.replace("[^A-Za-z0-9._-]".toRegex(), "_")
        val pluginDir = Paths.get(PathManager.getSystemPath(), "com.github.lucsartes.intellijprojectidentifierplugin", "watermarks")
        // Generate a short 10-char UUID-like token for uniqueness
        val uid10 = UUID.randomUUID().toString().replace("-", "").take(10)
        val baseName = "$safeProject-$safeText"
        val fileName = "$baseName-$uid10.png"
        return pluginDir.resolve(fileName)
    }
}
