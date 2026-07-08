package com.github.lucsartes.intellijprojectidentifierplugin.core

import java.awt.AlphaComposite
import java.awt.Color
import java.awt.Font
import java.awt.GraphicsEnvironment
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

/**
 * AWT-based renderer that produces a transparent PNG with the given text tightly cropped to the
 * top-left, plus a right and bottom margin.
 *
 * Uses only JDK AWT APIs, which work in headless mode, so it stays pure (no IntelliJ dependency)
 * and unit-testable. The image size adapts to the rendered text to avoid extra spacing.
 */
class ImageRenderer {

    fun renderPng(text: String, fontFamily: String? = null, fontSizePx: Int? = null, textColorArgb: Int? = null): ByteArray {
        val textToDraw = text.ifBlank { "" }

        // Determine effective font size and family (font policy lives in FontSupport)
        val effectiveFontSize = (fontSizePx ?: CoreDefaults.DEFAULT_FONT_SIZE_PX).coerceAtLeast(CoreDefaults.MIN_FONT_SIZE_PX)
        val availableFamilies = try {
            GraphicsEnvironment.getLocalGraphicsEnvironment().availableFontFamilyNames.toSet()
        } catch (t: Throwable) {
            emptySet()
        }
        val family = FontSupport.resolveFamily(fontFamily, availableFamilies)
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

            // Draw text fully opaque; on-screen opacity is controlled by IDE background settings only
            g2d.composite = AlphaComposite.SrcOver
            g2d.font = baseFont
            g2d.color = textColorArgb?.let { Color(it, true) } ?: Color.WHITE

            // Position so that the top touches the image's top and the left touches the image's left
            val x = 0
            val y = ascent // baseline so that top (y - ascent) == 0
            g2d.drawString(textToDraw, x, y)
        } finally {
            g2d.dispose()
        }

        return toPngBytes(image)
    }

    private fun toPngBytes(image: BufferedImage): ByteArray {
        val baos = ByteArrayOutputStream()
        ImageIO.write(image, "png", baos)
        return baos.toByteArray()
    }
}
