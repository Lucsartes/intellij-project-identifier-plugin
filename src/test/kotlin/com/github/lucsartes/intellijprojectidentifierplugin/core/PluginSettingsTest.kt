package com.github.lucsartes.intellijprojectidentifierplugin.core

import org.junit.Assert.*
import org.junit.Test

class PluginSettingsTest {

    @Test
    fun defaults_areNullAndDataClassCopyWorks() {
        val defaults = PluginSettings()
        assertNull(defaults.identifierOverride)
        assertNull(defaults.fontFamily)
        assertNull(defaults.fontSizePx)
        assertNull(defaults.textColorArgb)

        val s2 = defaults.copy(
            identifierOverride = "MyID",
            fontFamily = "SansSerif",
            fontSizePx = 123,
            textColorArgb = 0xFF00FF00.toInt(),
        )
        assertEquals("MyID", s2.identifierOverride)
        assertEquals("SansSerif", s2.fontFamily)
        assertEquals(123, s2.fontSizePx)
        assertEquals(0xFF00FF00.toInt(), s2.textColorArgb)
    }

    @Test
    fun equality_worksCorrectly() {
        val s1 = PluginSettings(identifierOverride = "ABC", fontFamily = "Arial", fontSizePx = 100, textColorArgb = 0xFFFFFF)
        val s2 = PluginSettings(identifierOverride = "ABC", fontFamily = "Arial", fontSizePx = 100, textColorArgb = 0xFFFFFF)
        val s3 = PluginSettings(identifierOverride = "XYZ", fontFamily = "Arial", fontSizePx = 100, textColorArgb = 0xFFFFFF)

        assertEquals(s1, s2)
        assertNotEquals(s1, s3)
        assertEquals(s1.hashCode(), s2.hashCode())
    }

    @Test
    fun partialCopy_preservesUnchangedValues() {
        val original = PluginSettings(identifierOverride = "TEST", fontFamily = "Courier", fontSizePx = 200, textColorArgb = 0xFF0000)
        val updated = original.copy(fontSizePx = 250)

        assertEquals("TEST", updated.identifierOverride)
        assertEquals("Courier", updated.fontFamily)
        assertEquals(250, updated.fontSizePx)
        assertEquals(0xFF0000, updated.textColorArgb)
    }

    @Test
    fun canHandleNegativeColorValues() {
        // ARGB colors with alpha channel can be negative when interpreted as signed int
        val settings = PluginSettings(textColorArgb = 0xFF000000.toInt()) // Black with full opacity
        assertTrue(settings.textColorArgb!! < 0) // Negative when treated as signed int
    }
}
