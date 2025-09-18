package com.github.lucsartes.intellijprojectidentifierplugin.core

import com.github.lucsartes.intellijprojectidentifierplugin.ports.ImageService
import com.intellij.ui.JBColor
import java.awt.AlphaComposite
import java.awt.Font
import java.awt.GraphicsEnvironment
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.logging.Logger
import javax.imageio.ImageIO

/**
 * AWT-based implementation of ImageService that renders a transparent PNG with
 * the provided text tightly cropped to the top-left, plus a 50px margin on the
 * right and a 10 px margin on the bottom.
 *
 * Notes:
 * - Uses only JDK AWT/Swing APIs, which work in headless mode.
 * - Image size adapts to the rendered text to avoid extra spacing.
 */
class ImageServiceImpl : ImageService {

    private val log: Logger = Logger.getLogger(ImageServiceImpl::class.java.name)


    override fun renderPng(text: String, fontFamily: String?, fontSizePx: Int?): ByteArray {
        val inputPreview = if (text.length > CoreDefaults.LOG_PREVIEW_LENGTH) text.take(CoreDefaults.LOG_PREVIEW_LENGTH) + "…" else text
        log.info("Rendering PNG for text (len=${text.length}): '$inputPreview'")

        val textToDraw = text.ifBlank { "" }

        // Determine effective font family and size
        val effectiveFontSize = (fontSizePx ?: CoreDefaults.DEFAULT_FONT_SIZE_PX).coerceAtLeast(CoreDefaults.MIN_FONT_SIZE_PX)
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

        // Canvas size: text flush to top-left, margins on right/bottom
        val width = (textWidth + CoreDefaults.MARGIN_RIGHT_PX).coerceAtLeast(1)
        val height = (ascent + descent + CoreDefaults.MARGIN_BOTTOM_PX).coerceAtLeast(1)

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
            g2d.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, CoreDefaults.WATERMARK_ALPHA)
            g2d.font = baseFont
            g2d.color = JBColor.WHITE

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
