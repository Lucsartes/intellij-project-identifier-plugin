package com.github.lucsartes.intellijprojectidentifierplugin.core

import org.junit.Assert.assertEquals
import org.junit.Test

class IdentifierGeneratorTest {

    private val service = IdentifierGenerator()

    @Test
    fun generate_mapsMyAwesomeProjectToMAP() {
        assertEquals("MAP", service.generate("My Awesome Project", emptySet()))
    }

    @Test
    fun generate_handlesMultipleSpaces() {
        assertEquals("MSH", service.generate("  many   spaces  here ", emptySet()))
    }

    @Test
    fun generate_handlesHyphensAndUnderscores() {
        assertEquals("FBB", service.generate("foo-bar_baz", emptySet()))
    }

    @Test
    fun generate_returnsEmptyForBlankOrNoAlphanumeric() {
        assertEquals("", service.generate("   ", emptySet()))
        assertEquals("", service.generate("!!!", emptySet()))
    }
}
