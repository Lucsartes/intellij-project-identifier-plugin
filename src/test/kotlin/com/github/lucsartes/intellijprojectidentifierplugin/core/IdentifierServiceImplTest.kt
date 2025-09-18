package com.github.lucsartes.intellijprojectidentifierplugin.core

import org.junit.Assert.assertEquals
import org.junit.Test

class IdentifierServiceImplTest {

    private val service = IdentifierServiceImpl()

    @Test
    fun generate_mapsMyAwesomeProjectToMAP() {
        assertEquals("MAP", service.generate("My Awesome Project"))
    }

    @Test
    fun generate_handlesMultipleSpaces() {
        assertEquals("MSH", service.generate("  many   spaces  here "))
    }

    @Test
    fun generate_handlesHyphensAndUnderscores() {
        assertEquals("FBB", service.generate("foo-bar_baz"))
    }

    @Test
    fun generate_returnsEmptyForBlankOrNoAlphanumeric() {
        assertEquals("", service.generate("   "))
        assertEquals("", service.generate("!!!"))
    }
}
