package com.github.lucsartes.intellijprojectidentifierplugin.core

/**
 * Core plugin settings. Pure domain model with no IntelliJ dependencies.
 *
 * Fields are intentionally minimal and intentionally exclude IDE-specific presentation options
 * like opacity, which are controlled by the IDE's own Appearance > Background Image settings.
 */
data class PluginSettings(
    val enabled: Boolean = true,
    /**
     * If provided, this text overrides the derived project identifier.
     * Null means use the automatically derived identifier.
     */
    val identifierOverride: String? = null
)
