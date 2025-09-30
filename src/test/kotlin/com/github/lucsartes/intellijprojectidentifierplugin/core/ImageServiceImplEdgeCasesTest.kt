package com.github.lucsartes.intellijprojectidentifierplugin.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

class ImageServiceImplEdgeCasesTest {

    private val service = ImageServiceImpl()

    @Test
    fun renderPng_returns1x1_whenTextIsBlank() {
        System.setProperty("java.awt.headless", "true")
        val bytes = service.renderPng("")
        ByteArrayInputStream(bytes).use { bais ->
            val img = ImageIO.read(bais)
            assertTrue("ImageIO failed to read generated PNG", img != null)
            assertEquals(1, img.width)
            assertEquals(1, img.height)
        }
    }

    @Test
    fun renderPng_appliesExplicitArgbColor() {
        System.setProperty("java.awt.headless", "true")
        val argb = 0xFF112233.toInt() // opaque custom color
        val bytes = service.renderPng("TEST", null, null, argb)
        ByteArrayInputStream(bytes).use { bais ->
            val img = ImageIO.read(bais)
            assertTrue("ImageIO failed to read generated PNG", img != null)

            // Find at least one fully opaque pixel with the exact RGB we requested
            var found = false
            outer@ for (y in 0 until img.height) {
                for (x in 0 until img.width) {
                    val pixel = img.getRGB(x, y)
                    val a = pixel ushr 24 and 0xFF
                    val rgb = pixel and 0x00FFFFFF
                    if (a == 0xFF && rgb == 0x00112233) {
                        found = true
                        break@outer
                    }
                }
            }
            assertTrue("Expected to find at least one pixel with exact ARGB color", found)
        }
    }

    @Test
    fun renderPng_handlesInvalidFontFamilyGracefully() {
        System.setProperty("java.awt.headless", "true")
        val bytes = service.renderPng("Sample", "DefinitelyNotARealFontFamilyName", 72, null)
        assertTrue(bytes.isNotEmpty())
        // Ensure decodable
        ByteArrayInputStream(bytes).use { bais ->
            val img = ImageIO.read(bais)
            assertTrue(img != null && img.width > 1 && img.height > 1)
        }
    }

    @Test
    fun renderPng_coercesTooSmallFontSize_andProducesPng() {
        System.setProperty("java.awt.headless", "true")
        val bytes = service.renderPng("x", null, 0, null)
        assertTrue(bytes.isNotEmpty())
        ByteArrayInputStream(bytes).use { bais ->
            val img = ImageIO.read(bais)
            assertTrue(img != null && img.width > 1 && img.height > 1)
        }
    }

    @Test
    fun renderPng_addsRightAndBottomMargins() {
        System.setProperty("java.awt.headless", "true")
        val bytes = service.renderPng("A", null, 48, null)
        ByteArrayInputStream(bytes).use { bais ->
            val img = ImageIO.read(bais)
            assertTrue(img != null)
            // For non-empty text, width should exceed right margin alone (textWidth >= 1)
            assertTrue("Width should exceed right margin for non-empty text", img.width > CoreDefaults.MARGIN_RIGHT_PX)
            // Height should be greater than margin bottom alone
            assertTrue("Height should exceed bottom margin for non-empty text", img.height > CoreDefaults.MARGIN_BOTTOM_PX)
        }
    }
}
