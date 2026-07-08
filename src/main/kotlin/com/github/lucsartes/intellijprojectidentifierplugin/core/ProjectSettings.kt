package com.github.lucsartes.intellijprojectidentifierplugin.core

/**
 * Per-project plugin settings. Pure domain model with no IntelliJ dependencies.
 *
 * Fields are intentionally minimal and deliberately exclude IDE-specific presentation options like
 * opacity, which are controlled by the IDE's own Appearance > Background Image settings.
 */
data class ProjectSettings(
    /**
     * If provided, this text overrides the derived project identifier.
     * Null means use the automatically derived identifier.
     */
    val identifierOverride: String? = null,
    /**
     * Optional font family to use when rendering the watermark text. Null means renderer default.
     */
    val fontFamily: String? = null,
    /**
     * Optional font size (in pixels) to use when rendering the watermark text. Null means renderer default.
     */
    val fontSizePx: Int? = null,
    /**
     * Optional ARGB color for the text. Null means default (white).
     */
    val textColorArgb: Int? = null,
)
