package com.github.lucsartes.intellijprojectidentifierplugin.core

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

class ImageServiceImplTest {

    private val service = ImageServiceImpl()

    @Test
    fun renderPng_returnsNonEmptyPngBytes_forBasicText() {
        System.setProperty("java.awt.headless", "true")

        val bytes = service.renderPng("Test", null, null)

        // Non-empty
        assertTrue("Expected non-empty bytes", bytes.isNotEmpty())

        // PNG signature check (first 8 bytes)
        assertTrue("Expected PNG signature", bytes.size >= 8)
        val sig = bytes.take(8).toByteArray()
        val expectedSig = byteArrayOf(
            0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte(),
            0x0D, 0x0A, 0x1A, 0x0A
        )
        assertTrue("PNG signature mismatch", sig.contentEquals(expectedSig))

        // Ensure ImageIO can read it
        ByteArrayInputStream(bytes).use { bais ->
            val img = ImageIO.read(bais)
            assertTrue("ImageIO failed to read generated PNG", img != null && img.width > 0 && img.height > 0)
        }
    }
}
