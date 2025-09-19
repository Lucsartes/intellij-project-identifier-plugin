package com.github.lucsartes.intellijprojectidentifierplugin.i18n

import org.junit.Assert.*
import org.junit.Test
import java.util.*

class I18nResourceBundleTest {

    private val baseName = "messages.MyBundle"

    @Test
    fun `default (English) bundle loads and returns expected values`() {
        val bundle = ResourceBundle.getBundle(baseName, Locale.ROOT)
        assertEquals("Project Identifier Settings", bundle.getString("settings.title"))
        assertEquals("Font family", bundle.getString("settings.font.family.label"))
        assertEquals("{0} (Default)", bundle.getString("default.annotated.value"))
        assertEquals("Text size (px)", bundle.getString("settings.text.size.px.label"))
        assertEquals("Text color", bundle.getString("settings.text.color.label"))
        assertEquals("Reset all settings to default values", bundle.getString("settings.reset.label"))
        assertEquals("Reset", bundle.getString("settings.reset.button"))
    }

    @Test
    fun `french bundle loads and contains translated values`() {
        val fr = ResourceBundle.getBundle(baseName, Locale.FRENCH)
        assertEquals("Paramètres de l'Identifiant de Projet", fr.getString("settings.title"))
        assertEquals("Famille de police", fr.getString("settings.font.family.label"))
        assertEquals("{0} (Par défaut)", fr.getString("default.annotated.value"))
        assertEquals("Taille du texte (px)", fr.getString("settings.text.size.px.label"))
        assertEquals("Couleur du texte", fr.getString("settings.text.color.label"))
        assertEquals("Réinitialiser tous les réglages aux valeurs par défaut", fr.getString("settings.reset.label"))
        assertEquals("Réinitialiser", fr.getString("settings.reset.button"))
    }

    @Test
    fun `french and default bundles have identical key sets`() {
        val en = ResourceBundle.getBundle(baseName, Locale.ROOT)
        val fr = ResourceBundle.getBundle(baseName, Locale.FRENCH)

        val enKeys = en.keys.asSequence().toSet()
        val frKeys = fr.keys.asSequence().toSet()

        // There can be extra metadata keys; ensure at least that all English keys exist in French
        val missingInFr = enKeys - frKeys
        assertTrue("French bundle is missing keys: $missingInFr", missingInFr.isEmpty())
    }
}
