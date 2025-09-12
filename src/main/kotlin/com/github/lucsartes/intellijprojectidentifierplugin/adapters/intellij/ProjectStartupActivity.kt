package com.github.lucsartes.intellijprojectidentifierplugin.adapters.intellij

import com.github.lucsartes.intellijprojectidentifierplugin.ports.IdentifierService
import com.github.lucsartes.intellijprojectidentifierplugin.ports.ImageService
import com.github.lucsartes.intellijprojectidentifierplugin.core.PluginSettings
import com.github.lucsartes.intellijprojectidentifierplugin.ports.BackgroundImagePort
import com.github.lucsartes.intellijprojectidentifierplugin.ports.SettingsPort
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Main entry point: runs on project open and orchestrates the pipeline
 * (generate identifier -> generate image -> save image -> set background).
 * It also listens to settings changes to re-run the pipeline.
 */
class ProjectStartupActivity : ProjectActivity {

    private val log = Logger.getInstance(ProjectStartupActivity::class.java)

    override suspend fun execute(project: Project) {
        log.info("Starting Project Identifier pipeline for project '${project.name}' on startup")
        // Run once on startup
        runCatching { runPipeline(project) }
            .onFailure { log.warn("Pipeline failed on project startup for '${project.name}'", it) }

        // Subscribe to settings changes and rerun the pipeline
        val connection = project.messageBus.connect(project)
        connection.subscribe(
            IntelliJSettingsService.TOPIC,
            object : IntelliJSettingsService.SettingsChangedListener {
                override fun settingsChanged(newSettings: PluginSettings) {
                    log.info("Settings changed for '${project.name}': enabled=${newSettings.enabled}, override=${newSettings.identifierOverride}")
                    runCatching { runPipeline(project) }
                        .onFailure { log.warn("Pipeline failed after settings change for '${project.name}'", it) }
                }
            }
        )
        log.info("Subscribed to Project Identifier settings changes for project '${project.name}'")
    }

    private fun runPipeline(project: Project) {
        log.info("Pipeline step: load settings for project '${project.name}'")
        val settings = ApplicationManager.getApplication().getService(SettingsPort::class.java).load()
        log.info("Settings loaded: enabled=${settings.enabled}, override=${settings.identifierOverride}")
        if (!settings.enabled) {
            log.info("Project Identifier is disabled; skipping background application for '${project.name}'")
            return
        }

        val projectName = project.name
        log.info("Pipeline step: derive identifier (projectName='$projectName', hasOverride=${settings.identifierOverride != null})")
        val identifierService = ApplicationManager.getApplication().getService(IdentifierService::class.java)
        val text = settings.identifierOverride ?: identifierService.generate(projectName)
        log.info("Identifier resolved: '$text'")

        log.info("Pipeline step: render PNG for identifier")
        val imageService = ApplicationManager.getApplication().getService(ImageService::class.java)
        val imageBytes = imageService.renderPng(text)
        log.info("Image rendered: ${imageBytes.size} bytes")

        log.info("Pipeline step: resolve image path")
        val imagePath = resolveImagePath(project, text)
        log.info("Image path resolved: '$imagePath'")

        // Ensure parent directory exists, cleanup previous files, and write the new file
        log.info("Pipeline step: write image to disk (with cleanup)")
        Files.createDirectories(imagePath.parent)
        // Best-effort cleanup to avoid stale files and force IntelliJ to reload when name changes
        runCatching {
            val safeProjectPrefix = project.name.replace("[^A-Za-z0-9._-]".toRegex(), "_")
            Files.list(imagePath.parent).use { stream ->
                stream.forEach { p ->
                    val name = p.fileName.toString()
                    if (name.startsWith(safeProjectPrefix)) {
                        runCatching { Files.deleteIfExists(p) }
                            .onFailure { t -> log.warn("Failed to delete old watermark file '$p'", t) }
                    }
                }
            }
        }.onFailure { t -> log.warn("Failed to cleanup watermark directory '${imagePath.parent}'", t) }

        Files.write(imagePath, imageBytes)
        log.info("Image written to '$imagePath' (${imageBytes.size} bytes)")

        // Apply via port
        log.info("Pipeline step: apply background via port")
        val backgroundSetter = project.getService(BackgroundImagePort::class.java)
        backgroundSetter.setBackgroundImage(imagePath)

        log.info("Applied project identifier watermark for '$projectName' at '$imagePath'")
    }

    private fun resolveImagePath(project: Project, text: String): Path {
        // Centralized, plugin-specific directory under IDE system path (cache-like storage)
        val safeProject = project.name.replace("[^A-Za-z0-9._-]".toRegex(), "_")
        val safeText = text.replace("[^A-Za-z0-9._-]".toRegex(), "_")
        val pluginDir = Paths.get(PathManager.getSystemPath(), "com.github.lucsartes.intellijprojectidentifierplugin", "watermarks")
        val fileName = if (safeText.isNotBlank()) "$safeProject - $safeText.png" else "$safeProject.png"
        return pluginDir.resolve(fileName)
    }
}
