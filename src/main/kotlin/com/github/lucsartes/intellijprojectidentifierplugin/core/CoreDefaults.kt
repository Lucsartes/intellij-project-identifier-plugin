package com.github.lucsartes.intellijprojectidentifierplugin.core

/**
 * Core-level defaults and constants used by domain and core implementations.
 * Keep this file free of IntelliJ SDK types.
 */
object CoreDefaults {
    // Logging preview length for long text values
    const val LOG_PREVIEW_LENGTH: Int = 64

    // Identifier tokenization: letters or digits (Unicode-aware)
    val TOKEN_REGEX: Regex = Regex("[\\p{L}\\p{N}]+")

    // Image rendering defaults
    const val DEFAULT_FONT_SIZE_PX: Int = 144 // equals ((108 * 4) / 3)
    const val MIN_FONT_SIZE_PX: Int = 1
    const val MARGIN_RIGHT_PX: Int = 50
    const val MARGIN_BOTTOM_PX: Int = 10
}