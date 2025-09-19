package com.github.lucsartes.intellijprojectidentifierplugin.i18n

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class PluginXmlI18nTest {

    private val pluginXml = File("src/main/resources/META-INF/plugin.xml")

    @Test
    fun `plugin xml declares resource-bundle and uses bundle-key for settings configurable`() {
        assertTrue("plugin.xml should exist at expected path", pluginXml.exists())

        val docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val doc = docBuilder.parse(pluginXml)
        doc.documentElement.normalize()

        val resourceBundleNodes = doc.getElementsByTagName("resource-bundle")
        assertTrue("plugin.xml must declare <resource-bundle>", resourceBundleNodes.length > 0)
        val bundleText = resourceBundleNodes.item(0).textContent.trim()
        assertEquals("messages.MyBundle", bundleText)

        val projectConfigurables = doc.getElementsByTagName("projectConfigurable")
        assertTrue("plugin.xml must have a <projectConfigurable> entry", projectConfigurables.length > 0)
        val cfg = projectConfigurables.item(0)
        val attrs = cfg.attributes

        assertNull("displayName attribute should not be used when bundle/key are provided", attrs.getNamedItem("displayName"))

        val bundleAttr = attrs.getNamedItem("bundle")
        val keyAttr = attrs.getNamedItem("key")
        assertNotNull("projectConfigurable should declare bundle attribute", bundleAttr)
        assertNotNull("projectConfigurable should declare key attribute", keyAttr)
        assertEquals("messages.MyBundle", bundleAttr!!.nodeValue)
        assertEquals("settings.title", keyAttr!!.nodeValue)
    }
}
