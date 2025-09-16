package com.github.lucsartes.intellijprojectidentifierplugin.core

import com.github.lucsartes.intellijprojectidentifierplugin.ports.ImageService
import java.awt.*
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.logging.Logger
import javax.imageio.ImageIO

/**
 * AWT-based implementation of ImageService that renders a transparent PNG with
 * the provided text tightly cropped to the top-left, plus a 50px margin on the
 * right and bottom.
 *
 * Notes:
 * - Uses only JDK AWT/Swing APIs, which work in headless mode.
 * - Image size adapts to the rendered text to avoid extra spacing.
 */
class ImageServiceImpl : ImageService {

    private val log: Logger = Logger.getLogger(ImageServiceImpl::class.java.name)

    override fun renderPng(text: String, fontFamily: String?, fontSizePx: Int?): ByteArray {
        val inputPreview = if (text.length > 64) text.take(64) + "…" else text
        log.info("Rendering PNG for text (len=${text.length}): '$inputPreview'")

        val textToDraw = text.ifBlank { "" }

        // Determine effective font family and size
        val defaultSize = ((108 * 4) / 3).coerceAtLeast(12)
        val effectiveFontSize = (fontSizePx ?: defaultSize).coerceAtLeast(1)
        val availableFamilies = try {
            GraphicsEnvironment.getLocalGraphicsEnvironment().availableFontFamilyNames.toSet()
        } catch (t: Throwable) {
            emptySet()
        }
        val family = fontFamily?.takeIf { it.isNotBlank() && (availableFamilies.isEmpty() || availableFamilies.contains(it)) }
            ?: "SansSerif"
        val baseFont = Font(family, Font.PLAIN, effectiveFontSize)

        // Use a temporary graphics context to measure text
        val tmpImage = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
        val tmpG = tmpImage.createGraphics()
        tmpG.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        tmpG.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        tmpG.font = baseFont
        val fm = tmpG.fontMetrics
        val ascent = fm.ascent
        val descent = fm.descent
        val textWidth = if (textToDraw.isNotEmpty()) fm.stringWidth(textToDraw) else 0
        tmpG.dispose()

        // If no text, return a minimal transparent PNG
        if (textToDraw.isEmpty()) {
            return toPngBytes(BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB))
        }

        // Canvas size: text flush to top-left, plus 50px on right and bottom
        val marginRight = 50
        val marginBottom = 10
        val width = (textWidth + marginRight).coerceAtLeast(1)
        val height = (ascent + descent + marginBottom).coerceAtLeast(1)

        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val g2d = image.createGraphics()

        try {
            // Enable antialiasing for better text quality
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

            // Clear to transparent
            g2d.composite = AlphaComposite.Clear
            g2d.fillRect(0, 0, width, height)

            // Draw text with low opacity for watermark effect
            g2d.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.15f)
            g2d.font = baseFont
            g2d.color = Color(255, 255, 255)

            // Position so that top touches image's top, left touches image's left
            val x = 0
            val y = ascent // baseline so that top (y - ascent) == 0
            g2d.drawString(textToDraw, x, y)
            log.info("Drew text at x=$x, y=$y; canvas=${width}x${height}; textWidth=$textWidth, ascent=$ascent, descent=$descent; fontFamily='$family', fontSize=$effectiveFontSize")
        } finally {
            g2d.dispose()
        }

        return toPngBytes(image)
    }

    private fun toPngBytes(image: BufferedImage): ByteArray {
        val baos = ByteArrayOutputStream()
        ImageIO.write(image, "png", baos)
        val bytes = baos.toByteArray()
        log.info("PNG rendered: ${bytes.size} bytes (size=${image.width}x${image.height})")
        return bytes
    }
}
