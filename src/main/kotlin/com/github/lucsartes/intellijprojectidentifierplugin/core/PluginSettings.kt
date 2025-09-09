package com.github.lucsartes.intellijprojectidentifierplugin.core

/**
 * Core plugin settings. Pure domain model with no IntelliJ dependencies.
 *
 * Fields are intentionally minimal for the initial core modeling and can be
 * extended by future settings UI/adapters without breaking the architecture.
 */
data class PluginSettings(
    val enabled: Boolean = true,
    /**
     * Watermark opacity in the range [0.0, 1.0].
     * Default is a subtle 8% opacity.
     */
    val opacity: Float = 0.08f,
    /**
     * If provided, this text overrides the derived project identifier.
     * Null means use the automatically derived identifier.
     */
    val identifierOverride: String? = null
) {
    init {
        require(opacity in 0.0f..1.0f) { "opacity must be within [0.0, 1.0]" }
    }
}