package com.github.lucsartes.intellijprojectidentifierplugin.core

import com.github.lucsartes.intellijprojectidentifierplugin.ports.IdentifierService
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests for IdentifierServiceImpl ignored words functionality.
 */
class IdentifierServiceImplIgnoredWordsTest {

    private val service: IdentifierService = IdentifierServiceImpl()

    @Test
    fun `generate ignores words in ignored list (case-insensitive)`() {
        val projectName = "tr-my-awesome-project"
        val ignoredWords = setOf("tr")
        val result = service.generate(projectName, ignoredWords)
        assertEquals("MAP", result)
    }

    @Test
    fun `generate ignores multiple words`() {
        val projectName = "tr-internal-my-project"
        val ignoredWords = setOf("tr", "internal")
        val result = service.generate(projectName, ignoredWords)
        assertEquals("MP", result)
    }

    @Test
    fun `generate is case-insensitive for ignored words`() {
        val projectName = "tr-My-Project"
        val ignoredWords = setOf("tr")
        val result = service.generate(projectName, ignoredWords)
        assertEquals("MP", result)
    }

    @Test
    fun `generate with mixed case ignored words`() {
        val projectName = "tre-my-project"
        val ignoredWords = setOf("trE")
        val result = service.generate(projectName, ignoredWords)
        assertEquals("MP", result)
    }

    @Test
    fun `generate returns empty when all words are ignored`() {
        val projectName = "tr-tre-internal"
        val ignoredWords = setOf("tr", "tre", "internal")
        val result = service.generate(projectName, ignoredWords)
        assertEquals("", result)
    }

    @Test
    fun `generate with empty ignored words behaves normally`() {
        val projectName = "tr-my-project"
        val ignoredWords = emptySet<String>()
        val result = service.generate(projectName, ignoredWords)
        assertEquals("TMP", result)
    }

    @Test
    fun `generate ignores only exact token matches`() {
        val projectName = "square-my-project"
        val ignoredWords = setOf("tr")
        val result = service.generate(projectName, ignoredWords)
        assertEquals("SMP", result)
    }

    @Test
    fun `generate with real-world example`() {
        val projectName = "tr-payment-service"
        val ignoredWords = setOf("tr", "tre")
        val result = service.generate(projectName, ignoredWords)
        assertEquals("PS", result)
    }

    @Test
    fun `generate with unicode and ignored words`() {
        val projectName = "tr-日本-project"
        val ignoredWords = setOf("tr")
        val result = service.generate(projectName, ignoredWords)
        assertEquals("日P", result)
    }

    @Test
    fun `generate with default parameter (no ignored words)`() {
        val projectName = "tr-my-project"
        val result = service.generate(projectName)
        assertEquals("TMP", result)
    }

    @Test
    fun `generate ignores word at beginning of project name`() {
        val projectName = "internal-backend-service"
        val ignoredWords = setOf("internal")
        val result = service.generate(projectName, ignoredWords)
        assertEquals("BS", result)
    }

    @Test
    fun `generate ignores word at end of project name`() {
        val projectName = "my-project-internal"
        val ignoredWords = setOf("internal")
        val result = service.generate(projectName, ignoredWords)
        assertEquals("MP", result)
    }

    @Test
    fun `generate ignores word in middle of project name`() {
        val projectName = "my-internal-project"
        val ignoredWords = setOf("internal")
        val result = service.generate(projectName, ignoredWords)
        assertEquals("MP", result)
    }

    @Test
    fun `generate with whitespace in ignored words is exact match`() {
        val projectName = "tr-my-project"
        val ignoredWords = setOf(" tr ", "my")
        val result = service.generate(projectName, ignoredWords)
        // " tr " doesn't match "tr", so "tr" is included
        assertEquals("TP", result)
    }

    @Test
    fun `generate with special characters in ignored words`() {
        val projectName = "my-prefix-awesome-project"
        // Hyphen is a separator, so "my-prefix" splits into tokens "my" and "prefix"
        // To ignore them, both need to be in the ignored words set
        val ignoredWords = setOf("my", "prefix")
        val result = service.generate(projectName, ignoredWords)
        assertEquals("AP", result)
    }

    @Test
    fun `generate with underscore separators and ignored words`() {
        val projectName = "tr_my_awesome_project"
        val ignoredWords = setOf("tr")
        val result = service.generate(projectName, ignoredWords)
        assertEquals("MAP", result)
    }

    @Test
    fun `generate with dot separators and ignored words`() {
        val projectName = "tr.my.awesome.project"
        val ignoredWords = setOf("tr")
        val result = service.generate(projectName, ignoredWords)
        assertEquals("MAP", result)
    }

    @Test
    fun `generate with camelCase and ignored words`() {
        val projectName = "trMyAwesomeProject"
        val ignoredWords = setOf("trMyAwesomeProject")
        val result = service.generate(projectName, ignoredWords)
        // In camelCase, "trMyAwesomeProject" is ONE token, so we can only ignore the entire token
        // To ignore "tr" and keep rest, the string should be separated: "tr-MyAwesomeProject"
        assertEquals("", result)
    }

    @Test
    fun `generate with multiple consecutive ignored words`() {
        val projectName = "tr-tre-test-my-project"
        val ignoredWords = setOf("tr", "tre", "test")
        val result = service.generate(projectName, ignoredWords)
        assertEquals("MP", result)
    }

    @Test
    fun `generate with ignored words containing numbers`() {
        val projectName = "v1-my-project-v2"
        val ignoredWords = setOf("v1", "v2")
        val result = service.generate(projectName, ignoredWords)
        assertEquals("MP", result)
    }

    @Test
    fun `generate with single word project name that is ignored`() {
        val projectName = "internal"
        val ignoredWords = setOf("internal")
        val result = service.generate(projectName, ignoredWords)
        assertEquals("", result)
    }

    @Test
    fun `generate with large set of ignored words`() {
        val projectName = "prefix1-prefix2-my-actual-project-suffix1"
        val ignoredWords = (1..50).map { "prefix$it" }.toSet() + setOf("suffix1")
        val result = service.generate(projectName, ignoredWords)
        assertEquals("MAP", result)
    }

    @Test
    fun `generate case insensitive with uppercase project name`() {
        val projectName = "TR-MY-PROJECT"
        val ignoredWords = setOf("tr")
        val result = service.generate(projectName, ignoredWords)
        assertEquals("MP", result)
    }

    @Test
    fun `generate case insensitive with lowercase ignored words`() {
        val projectName = "TR-MY-PROJECT"
        val ignoredWords = setOf("tr", "my")
        val result = service.generate(projectName, ignoredWords)
        assertEquals("P", result)
    }

    @Test
    fun `generate with duplicate words in project name and ignored list`() {
        val projectName = "test-test-project"
        val ignoredWords = setOf("test")
        val result = service.generate(projectName, ignoredWords)
        assertEquals("P", result)
    }

    @Test
    fun `generate preserves behavior with empty string in ignored words`() {
        val projectName = "my-project"
        val ignoredWords = setOf("", "my")
        val result = service.generate(projectName, ignoredWords)
        // Empty string should not match anything meaningful
        assertEquals("P", result)
    }
}
