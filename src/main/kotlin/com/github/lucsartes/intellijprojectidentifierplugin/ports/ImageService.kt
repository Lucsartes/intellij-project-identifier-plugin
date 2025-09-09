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
     * @return PNG image bytes.
     */
    fun renderPng(text: String): ByteArray
}