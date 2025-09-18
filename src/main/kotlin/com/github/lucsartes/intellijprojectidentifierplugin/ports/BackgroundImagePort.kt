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
}