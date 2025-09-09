package com.github.lucsartes.intellijprojectidentifierplugin.adapters.intellij

import com.github.lucsartes.intellijprojectidentifierplugin.core.IdentifierServiceImpl
import com.github.lucsartes.intellijprojectidentifierplugin.core.ImageServiceImpl
import com.github.lucsartes.intellijprojectidentifierplugin.core.PluginSettings
import com.github.lucsartes.intellijprojectidentifierplugin.ports.BackgroundImagePort
import com.github.lucsartes.intellijprojectidentifierplugin.ports.SettingsPort
import com.intellij.openapi.application.ApplicationManager
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
        // Run once on startup
        runCatching { runPipeline(project) }
            .onFailure { log.warn("Pipeline failed on project startup for '${'$'}{project.name}'", it) }

        // Subscribe to settings changes and rerun the pipeline
        val connection = project.messageBus.connect(project)
        connection.subscribe(
            IntelliJSettingsService.TOPIC,
            object : IntelliJSettingsService.SettingsChangedListener {
                override fun settingsChanged(newSettings: PluginSettings) {
                    runCatching { runPipeline(project) }
                        .onFailure { log.warn("Pipeline failed after settings change for '${'$'}{project.name}'", it) }
                }
            }
        )
    }

    private fun runPipeline(project: Project) {
        val settings = ApplicationManager.getApplication().getService(SettingsPort::class.java).load()
        if (!settings.enabled) {
            log.info("Project Identifier is disabled; skipping background application for '${'$'}{project.name}'")
            return
        }

        val projectName = project.name
        val text = settings.identifierOverride ?: IdentifierServiceImpl().generate(projectName)

        val imageBytes = ImageServiceImpl().renderPng(text)
        val imagePath = resolveImagePath(project)

        // Ensure parent directory exists and write the file
        Files.createDirectories(imagePath.parent)
        Files.write(imagePath, imageBytes)

        // Apply via port
        val backgroundSetter = project.getService(BackgroundImagePort::class.java)
        backgroundSetter.setBackgroundImage(imagePath)

        log.info("Applied project identifier watermark for '${'$'}projectName' at '${'$'}imagePath'")
    }

    private fun resolveImagePath(project: Project): Path {
        val basePath = project.basePath
        return if (basePath != null) {
            Paths.get(basePath, ".idea", "project-identifier", "watermark.png")
        } else {
            val safeName = project.name.replace("[^A-Za-z0-9._-]".toRegex(), "_")
            Paths.get(System.getProperty("java.io.tmpdir"), "project-identifier", safeName, "watermark.png")
        }
    }
}
