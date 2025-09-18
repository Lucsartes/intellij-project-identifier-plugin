package com.github.lucsartes.intellijprojectidentifierplugin.adapters.intellij

/**
 * Constants for interacting with IntelliJ background image settings.
 * These keys and defaults are adapter-level concerns and may reflect unstable/internal API.
 */
object BackgroundPropertiesConstants {
    // Legacy/auxiliary keys used by Settings UI
    const val KEY_OPACITY: String = "ide.background.opacity"
    const val KEY_FILL: String = "ide.background.fill"
    const val KEY_ANCHOR: String = "ide.background.anchor"
    const val KEY_FOR_PROJECT: String = "ide.background.for.project"

    // Default values when none are configured
    const val DEFAULT_OPACITY_PERCENT: Int = 15
    const val MIN_EFFECTIVE_OPACITY_PERCENT: Int = 0
    const val DEFAULT_STYLE: String = "plain"
    const val DEFAULT_ANCHOR: String = "bottom_right"
}