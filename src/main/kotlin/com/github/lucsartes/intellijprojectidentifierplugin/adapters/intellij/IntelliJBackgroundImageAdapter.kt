package com.github.lucsartes.intellijprojectidentifierplugin.adapters.intellij

import com.github.lucsartes.intellijprojectidentifierplugin.ports.BackgroundImagePort
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.impl.IdeBackgroundUtil
import java.nio.file.Path

/**
 * IntelliJ-specific adapter that applies editor background image settings by writing
 * to the private IDE properties keys. This isolates usage of unstable API.
 */
class IntelliJBackgroundImageAdapter(private val project: Project) : BackgroundImagePort {

    private val log = Logger.getInstance(IntelliJBackgroundImageAdapter::class.java)

    private data class PropsResult(val props: PropertiesComponent, val projectScoped: Boolean)

    private fun getPropertiesComponent(): PropsResult {
        var projectScoped = true
        val props = try {
            PropertiesComponent.getInstance(project)
        } catch (e: Throwable) {
            log.warn("Project-scoped PropertiesComponent not available, falling back to application scope", e)
            projectScoped = false
            PropertiesComponent.getInstance()
        }
        return PropsResult(props, projectScoped)
    }

    override fun setBackgroundImage(imagePath: Path) {
        try {
            val absolutePath = imagePath.toAbsolutePath().toString()

            // Prefer project-scoped properties when available
            val (props, projectScoped) = getPropertiesComponent()

            // Read existing editor background property to preserve user-customized options (opacity, style, anchor)
            val existing = props.getValue(IdeBackgroundUtil.EDITOR_PROP)?.trim()

            // Determine effective values and defaults
            // New logic: if opacity already set, use it; otherwise default to 15
            val (effectiveOpacity, effectiveStyle, effectiveAnchor) = run {
                val parts = existing?.split(',')
                val existingOpacityFromCombined = parts?.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 100)
                val existingOpacityFromLegacy = props.getValue(BackgroundPropertiesConstants.KEY_OPACITY)?.toIntOrNull()?.coerceIn(0, 100)
                val op = existingOpacityFromCombined ?: existingOpacityFromLegacy ?: BackgroundPropertiesConstants.DEFAULT_OPACITY_PERCENT
                val st = parts?.getOrNull(2)?.ifBlank { null } ?: BackgroundPropertiesConstants.DEFAULT_STYLE
                val an = parts?.getOrNull(3)?.ifBlank { null } ?: BackgroundPropertiesConstants.DEFAULT_ANCHOR
                Triple(op, st, an)
            }

            val newProp = "$absolutePath,$effectiveOpacity,$effectiveStyle,$effectiveAnchor"

            // Set combined property using IdeBackgroundUtil.EDITOR_PROP
            props.setValue(IdeBackgroundUtil.EDITOR_PROP, newProp)

            // Also set individual keys so the Settings UI stays in sync
            try {
                props.setValue(BackgroundPropertiesConstants.KEY_OPACITY, effectiveOpacity.toString())
                props.setValue(BackgroundPropertiesConstants.KEY_FILL, effectiveStyle)
                props.setValue(BackgroundPropertiesConstants.KEY_ANCHOR, effectiveAnchor)
                props.setValue(BackgroundPropertiesConstants.KEY_FOR_PROJECT, projectScoped.toString())
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

    override fun resetBackgroundSettingsToDefaults() {
        try {
            val (props, projectScoped) = getPropertiesComponent()

            val existing = props.getValue(IdeBackgroundUtil.EDITOR_PROP)?.trim()
            val existingPath = existing?.split(',')?.getOrNull(0).orEmpty()

            val newProp = listOf(
                existingPath,
                BackgroundPropertiesConstants.DEFAULT_OPACITY_PERCENT.toString(),
                BackgroundPropertiesConstants.DEFAULT_STYLE,
                BackgroundPropertiesConstants.DEFAULT_ANCHOR
            ).joinToString(",")

            // Write combined property (keeps current path if present)
            props.setValue(IdeBackgroundUtil.EDITOR_PROP, newProp)

            // Write legacy/UI keys
            try {
                props.setValue(BackgroundPropertiesConstants.KEY_OPACITY, BackgroundPropertiesConstants.DEFAULT_OPACITY_PERCENT.toString())
                props.setValue(BackgroundPropertiesConstants.KEY_FILL, BackgroundPropertiesConstants.DEFAULT_STYLE)
                props.setValue(BackgroundPropertiesConstants.KEY_ANCHOR, BackgroundPropertiesConstants.DEFAULT_ANCHOR)
                props.setValue(BackgroundPropertiesConstants.KEY_FOR_PROJECT, projectScoped.toString())
            } catch (ignored: Throwable) {
                log.debug("Failed to reset one of the legacy background properties", ignored)
            }

            log.info("Reset background properties to defaults for project '${project.name}' -> property='$newProp'")
            IdeBackgroundUtil.repaintAllWindows()
        } catch (t: Throwable) {
            log.warn("Failed to reset background image properties to defaults", t)
        }
    }
}
