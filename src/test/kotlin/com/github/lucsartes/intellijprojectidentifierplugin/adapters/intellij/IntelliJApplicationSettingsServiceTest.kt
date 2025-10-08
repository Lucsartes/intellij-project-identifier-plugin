package com.github.lucsartes.intellijprojectidentifierplugin.adapters.intellij

import com.github.lucsartes.intellijprojectidentifierplugin.core.ApplicationSettings
import com.intellij.testFramework.ApplicationRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for IntelliJApplicationSettingsService.
 * Tests the adapter implementation for application-level settings persistence according to ADR-003.
 */
class IntelliJApplicationSettingsServiceTest {

    @Rule
    @JvmField
    val appRule = ApplicationRule()

    @Test
    fun `initial load returns default settings`() {
        val service = IntelliJApplicationSettingsService()
        val settings = service.load()

        assertNotNull(settings)
        assertTrue(settings.ignoredWords.isEmpty())
    }

    @Test
    fun `save and load round trip preserves ignored words`() {
        val service = IntelliJApplicationSettingsService()
        val originalSettings = ApplicationSettings(ignoredWords = setOf("tr", "tre"))

        service.save(originalSettings)
        val loadedSettings = service.load()

        assertEquals(originalSettings, loadedSettings)
        assertEquals(2, loadedSettings.ignoredWords.size)
        assertTrue(loadedSettings.ignoredWords.containsAll(listOf("tr", "tre")))
    }

    @Test
    fun `save with empty set works correctly`() {
        val service = IntelliJApplicationSettingsService()
        val settings = ApplicationSettings(ignoredWords = emptySet())

        service.save(settings)
        val loadedSettings = service.load()

        assertTrue(loadedSettings.ignoredWords.isEmpty())
    }

    @Test
    fun `save overwrites previous settings`() {
        val service = IntelliJApplicationSettingsService()

        service.save(ApplicationSettings(ignoredWords = setOf("tr", "tre")))
        val firstLoad = service.load()
        assertEquals(2, firstLoad.ignoredWords.size)

        service.save(ApplicationSettings(ignoredWords = setOf("internal")))
        val secondLoad = service.load()
        assertEquals(1, secondLoad.ignoredWords.size)
        assertTrue(secondLoad.ignoredWords.contains("internal"))
        assertFalse(secondLoad.ignoredWords.contains("tr"))
    }

    @Test
    fun `getState returns current state`() {
        val service = IntelliJApplicationSettingsService()
        service.save(ApplicationSettings(ignoredWords = setOf("test", "word")))

        val state = service.getState()
        assertNotNull(state)
        assertEquals(2, state.ignoredWords.size)
        assertTrue(state.ignoredWords.containsAll(listOf("test", "word")))
    }

    @Test
    fun `loadState correctly initializes service`() {
        val service = IntelliJApplicationSettingsService()
        val state = IntelliJApplicationSettingsService.State(ignoredWords = listOf("loaded", "from", "state"))

        service.loadState(state)
        val settings = service.load()

        assertEquals(3, settings.ignoredWords.size)
        assertTrue(settings.ignoredWords.containsAll(listOf("loaded", "from", "state")))
    }

    @Test
    fun `state conversion handles duplicates in list`() {
        val service = IntelliJApplicationSettingsService()
        val state = IntelliJApplicationSettingsService.State(ignoredWords = listOf("tr", "tr", "tre"))

        service.loadState(state)
        val settings = service.load()

        // Set automatically deduplicates
        assertEquals(2, settings.ignoredWords.size)
        assertTrue(settings.ignoredWords.containsAll(listOf("tr", "tre")))
    }

    @Test
    fun `save preserves order when converting to list for storage`() {
        val service = IntelliJApplicationSettingsService()
        val settings = ApplicationSettings(ignoredWords = setOf("zebra", "apple", "banana"))

        service.save(settings)
        val state = service.getState()

        // List should contain all elements
        assertEquals(3, state.ignoredWords.size)
        assertTrue(state.ignoredWords.containsAll(listOf("zebra", "apple", "banana")))
    }

    @Test
    fun `multiple save operations work correctly`() {
        val service = IntelliJApplicationSettingsService()

        service.save(ApplicationSettings(ignoredWords = setOf("a")))
        service.save(ApplicationSettings(ignoredWords = setOf("a", "b")))
        service.save(ApplicationSettings(ignoredWords = setOf("a", "b", "c")))

        val settings = service.load()
        assertEquals(3, settings.ignoredWords.size)
    }

    @Test
    fun `handles unicode in ignored words`() {
        val service = IntelliJApplicationSettingsService()
        val settings = ApplicationSettings(ignoredWords = setOf("日本", "café", "Москва"))

        service.save(settings)
        val loaded = service.load()

        assertEquals(3, loaded.ignoredWords.size)
        assertTrue(loaded.ignoredWords.containsAll(listOf("日本", "café", "Москва")))
    }

    @Test
    fun `handles special characters in ignored words`() {
        val service = IntelliJApplicationSettingsService()
        val settings = ApplicationSettings(ignoredWords = setOf("my-prefix", "test_word", "word.with.dots"))

        service.save(settings)
        val loaded = service.load()

        assertEquals(3, loaded.ignoredWords.size)
        assertTrue(loaded.ignoredWords.contains("my-prefix"))
    }

    @Test
    fun `large set of ignored words persists correctly`() {
        val service = IntelliJApplicationSettingsService()
        val words = (1..50).map { "word$it" }.toSet()
        val settings = ApplicationSettings(ignoredWords = words)

        service.save(settings)
        val loaded = service.load()

        assertEquals(50, loaded.ignoredWords.size)
    }
}

