package com.github.lucsartes.intellijprojectidentifierplugin.core

import com.github.lucsartes.intellijprojectidentifierplugin.ports.ImageService
import java.awt.*
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

/**
 * AWT-based implementation of ImageService that renders a transparent PNG with
 * the provided text centered on the image.
 *
 * Notes:
 * - Uses only JDK AWT/Swing APIs, which work in headless mode.
 * - Keeps implementation simple and deterministic with fixed canvas size.
 */
class ImageServiceImpl : ImageService {

    override fun renderPng(text: String): ByteArray {
        // Canvas configuration
        val width = 800
        val height = 600
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val g2d = image.createGraphics()

        try {
            // Enable antialiasing for better text quality
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

            // Transparent background (already transparent due to TYPE_INT_ARGB), but ensure it's cleared
            g2d.composite = AlphaComposite.Clear
            g2d.fillRect(0, 0, width, height)

            // Draw text with low opacity for watermark effect
            g2d.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.15f)

            // Choose a font size proportionally to canvas height
            val fontSize = (height * 0.18).toInt().coerceAtLeast(12)
            g2d.font = Font("SansSerif", Font.BOLD, fontSize)
            g2d.color = Color(0, 0, 0) // black text with alpha via composite

            val fm = g2d.fontMetrics
            val textToDraw = text.ifBlank { "" }

            // Compute centered position
            val textWidth = fm.stringWidth(textToDraw)
            val x = ((width - textWidth) / 2.0).toInt()
            val y = ((height - fm.height) / 2.0 + fm.ascent).toInt()

            // Draw only if there is text; otherwise keep it transparent
            if (textToDraw.isNotEmpty()) {
                g2d.drawString(textToDraw, x, y)
            }
        } finally {
            g2d.dispose()
        }

        val baos = ByteArrayOutputStream()
        ImageIO.write(image, "png", baos)
        return baos.toByteArray()
    }
}
