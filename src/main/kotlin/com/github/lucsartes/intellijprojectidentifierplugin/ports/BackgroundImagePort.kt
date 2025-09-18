package com.github.lucsartes.intellijprojectidentifierplugin.ports

import java.nio.file.Path

/**
 * Port to set the IDE/editor background image.
 * Implementations must isolate any unstable IDE APIs and fail gracefully.
 */
interface BackgroundImagePort {
    /**
     * Apply the background image from the given PNG file path.
     */
    fun setBackgroundImage(imagePath: Path)

    /**
     * Reset background-related settings (opacity, fill style, anchor) to adapter defaults,
     * so the Background Image settings UI reflects default values again.
     * Implementations should avoid throwing and fail gracefully.
     */
    fun resetBackgroundSettingsToDefaults()
}