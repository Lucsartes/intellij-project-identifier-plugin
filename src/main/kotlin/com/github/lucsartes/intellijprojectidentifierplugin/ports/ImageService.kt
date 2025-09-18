package com.github.lucsartes.intellijprojectidentifierplugin.ports

/**
 * Port responsible for rendering a transparent PNG image containing the given text.
 * Implementations may choose any rendering technology; this contract remains pure.
 */
interface ImageService {
    /**
     * Render the provided text into a PNG image with transparency.
     *
     * @param text The text content to render.
     * @param fontFamily Optional font family name to use; null means implementation default.
     * @param fontSizePx Optional font size in pixels; null means implementation default.
     * @param textColorArgb Optional 32-bit ARGB color for the text; null means implementation default (white).
     * @return PNG image bytes.
     */
    fun renderPng(text: String, fontFamily: String? = null, fontSizePx: Int? = null, textColorArgb: Int? = null): ByteArray
}