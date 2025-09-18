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
}
