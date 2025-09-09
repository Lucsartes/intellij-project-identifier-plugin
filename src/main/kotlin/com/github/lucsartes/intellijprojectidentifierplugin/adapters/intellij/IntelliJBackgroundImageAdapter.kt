package com.github.lucsartes.intellijprojectidentifierplugin.adapters.intellij

import com.github.lucsartes.intellijprojectidentifierplugin.ports.BackgroundImagePort
import com.github.lucsartes.intellijprojectidentifierplugin.ports.SettingsPort
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import java.nio.file.Path

/**
 * IntelliJ-specific adapter that applies editor background image settings by writing
 * to the private IDE properties keys. This isolates usage of unstable API.
 */
class IntelliJBackgroundImageAdapter(private val project: Project) : BackgroundImagePort {

    private val log = Logger.getInstance(IntelliJBackgroundImageAdapter::class.java)

    override fun setBackgroundImage(imagePath: Path) {
        try {
            val absolutePath = imagePath.toAbsolutePath().toString()

            // Read opacity from settings (domain) to keep port interface simple
            val settings = runCatching {
                ApplicationManager.getApplication().getService(SettingsPort::class.java).load()
            }.getOrNull()
            val opacity = (settings?.opacity ?: 0.08f).coerceIn(0.0f, 1.0f)

            // Prefer project-scoped properties when available
            val props = try {
                PropertiesComponent.getInstance(project)
            } catch (e: Throwable) {
                log.warn("Project-scoped PropertiesComponent not available, falling back to application scope", e)
                PropertiesComponent.getInstance()
            }

            // Keys are private/unstable; guard with try/catch and log failures
            props.setValue("ide.background.image", absolutePath)
            props.setValue("ide.background.image.opacity", opacity.toString())

            log.info("Applied background image for project '${'$'}{project.name}' -> path='${'$'}absolutePath', opacity=${'$'}opacity")
        } catch (t: Throwable) {
            // Fail gracefully: log and avoid throwing to keep IDE stable
            log.warn("Failed to apply background image via PropertiesComponent", t)
        }
    }
}
