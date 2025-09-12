package com.github.lucsartes.intellijprojectidentifierplugin.adapters.intellij

import com.github.lucsartes.intellijprojectidentifierplugin.ports.BackgroundImagePort
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.wm.impl.IdeBackgroundUtil
import java.io.File
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

            // Prefer project-scoped properties when available
            var projectScoped = true
            val props = try {
                PropertiesComponent.getInstance(project)
            } catch (e: Throwable) {
                log.warn("Project-scoped PropertiesComponent not available, falling back to application scope", e)
                projectScoped = false
                PropertiesComponent.getInstance()
            }

            // Read existing editor background property to preserve user-customized options (opacity, style, anchor)
            val existing = props.getValue(IdeBackgroundUtil.EDITOR_PROP)?.trim()

            // Default values if none exist
            val defaultOpacityPercent = 50
            val defaultStyle = "plain"
            val defaultAnchor = "bottom_right"

            // Determine effective values
            val (effectiveOpacity, effectiveStyle, effectiveAnchor) = if (!existing.isNullOrBlank()) {
                // Expected format: "<path>,<opacity%>,<style>,<anchor>"
                val parts = existing.split(',')
                val parsedOpacity = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 100)
                val rawOpacity = parsedOpacity ?: defaultOpacityPercent
                val op = if (rawOpacity >= 50) rawOpacity else 50
                val st = parts.getOrNull(2)?.ifBlank { null } ?: defaultStyle
                val an = parts.getOrNull(3)?.ifBlank { null } ?: defaultAnchor
                Triple(op, st, an)
            } else {
                val op = if (defaultOpacityPercent >= 50) defaultOpacityPercent else 50
                Triple(op, defaultStyle, defaultAnchor)
            }

            val newProp = "$absolutePath,$effectiveOpacity,$effectiveStyle,$effectiveAnchor"

            // Set combined property using IdeBackgroundUtil.EDITOR_PROP
            props.setValue(IdeBackgroundUtil.EDITOR_PROP, newProp)

            // Also set individual keys so the Settings UI stays in sync
            try {
                props.setValue("ide.background.opacity", effectiveOpacity.toString())
                props.setValue("ide.background.fill", effectiveStyle)
                props.setValue("ide.background.anchor", effectiveAnchor)
                props.setValue("ide.background.for.project", projectScoped.toString())
            } catch (ignored: Throwable) {
                // Best-effort sync with Settings UI; ignore failures of legacy keys
                log.debug("Failed to set one of the legacy background properties", ignored)
            }

            log.info("Applied background image for project '${project.name}' -> property='$newProp'")

            IdeBackgroundUtil.repaintAllWindows()
        } catch (t: Throwable) {
            // Fail gracefully: log and avoid throwing to keep IDE stable
            log.warn("Failed to apply background image via PropertiesComponent", t)
        }
    }
}
