package com.github.lucsartes.intellijprojectidentifierplugin.core

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for ApplicationSettings domain model.
 * Tests the application-level (global) settings according to ADR-003.
 */
class ApplicationSettingsTest {

    @Test
    fun `defaults are empty set`() {
        val defaults = ApplicationSettings()
        assertNotNull(defaults.ignoredWords)
        assertTrue(defaults.ignoredWords.isEmpty())
    }

    @Test
    fun `can create with single ignored word`() {
        val settings = ApplicationSettings(ignoredWords = setOf("tr"))
        assertEquals(1, settings.ignoredWords.size)
        assertTrue(settings.ignoredWords.contains("tr"))
    }

    @Test
    fun `can create with multiple ignored words`() {
        val settings = ApplicationSettings(ignoredWords = setOf("tr", "tre", "internal"))
        assertEquals(3, settings.ignoredWords.size)
        assertTrue(settings.ignoredWords.containsAll(listOf("tr", "tre", "internal")))
    }

    @Test
    fun `ignored words are case-sensitive in storage but used case-insensitively`() {
        // The set stores exact values as provided
        val settings = ApplicationSettings(ignoredWords = setOf("TR", "tr", "Tr"))
        // Set naturally deduplicates, but these are different strings
        assertEquals(3, settings.ignoredWords.size)
    }

    @Test
    fun `data class copy works correctly`() {
        val original = ApplicationSettings(ignoredWords = setOf("tr"))
        val updated = original.copy(ignoredWords = setOf("tr", "tre"))

        assertEquals(1, original.ignoredWords.size)
        assertEquals(2, updated.ignoredWords.size)
        assertTrue(updated.ignoredWords.contains("tre"))
    }

    @Test
    fun `equality works correctly`() {
        val settings1 = ApplicationSettings(ignoredWords = setOf("tr", "tre"))
        val settings2 = ApplicationSettings(ignoredWords = setOf("tr", "tre"))
        val settings3 = ApplicationSettings(ignoredWords = setOf("tr"))

        assertEquals(settings1, settings2)
        assertNotEquals(settings1, settings3)
        assertEquals(settings1.hashCode(), settings2.hashCode())
    }

    @Test
    fun `equality is order-independent for sets`() {
        val settings1 = ApplicationSettings(ignoredWords = setOf("tr", "tre", "internal"))
        val settings2 = ApplicationSettings(ignoredWords = setOf("internal", "tr", "tre"))

        assertEquals(settings1, settings2)
    }

    @Test
    fun `empty set is equal to default`() {
        val settings1 = ApplicationSettings()
        val settings2 = ApplicationSettings(ignoredWords = emptySet())

        assertEquals(settings1, settings2)
    }

    @Test
    fun `can handle special characters in ignored words`() {
        val settings = ApplicationSettings(ignoredWords = setOf("my-prefix", "test_word", "word.with.dots"))
        assertEquals(3, settings.ignoredWords.size)
        assertTrue(settings.ignoredWords.contains("my-prefix"))
    }

    @Test
    fun `can handle unicode in ignored words`() {
        val settings = ApplicationSettings(ignoredWords = setOf("日本", "café"))
        assertEquals(2, settings.ignoredWords.size)
        assertTrue(settings.ignoredWords.contains("日本"))
    }

    @Test
    fun `empty strings can be stored but are not meaningful`() {
        // The model doesn't validate - validation should happen at the UI/adapter layer
        val settings = ApplicationSettings(ignoredWords = setOf("", "tr"))
        assertEquals(2, settings.ignoredWords.size)
    }

    @Test
    fun `whitespace-only strings can be stored`() {
        // The model doesn't validate - validation should happen at the UI/adapter layer
        val settings = ApplicationSettings(ignoredWords = setOf("  ", "tr"))
        assertEquals(2, settings.ignoredWords.size)
    }

    @Test
    fun `large set of ignored words is supported`() {
        val words = (1..100).map { "word$it" }.toSet()
        val settings = ApplicationSettings(ignoredWords = words)
        assertEquals(100, settings.ignoredWords.size)
    }
}

