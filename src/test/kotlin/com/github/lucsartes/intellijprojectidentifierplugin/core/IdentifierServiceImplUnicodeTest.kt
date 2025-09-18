package com.github.lucsartes.intellijprojectidentifierplugin.core

import org.junit.Assert.assertEquals
import org.junit.Test

class IdentifierServiceImplUnicodeTest {

    private val service = IdentifierServiceImpl()

    @Test
    fun generate_handlesUnicodeLettersAndDigits() {
        // Mix of CJK, digits attached to letters, and Greek letters
        assertEquals("日語Α", service.generate("日本 語123 αβγ"))
        // Leading digits within a token should produce that digit
        assertEquals("1A", service.generate("123 abc"))
    }
}
