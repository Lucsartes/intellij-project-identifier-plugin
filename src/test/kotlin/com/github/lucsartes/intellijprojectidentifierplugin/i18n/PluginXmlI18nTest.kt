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

        val applicationConfigurables = doc.getElementsByTagName("applicationConfigurable")
        assertTrue("plugin.xml must have a <applicationConfigurable> entry", applicationConfigurables.length > 0)
        val parent = applicationConfigurables.item(0)
        val parentAttrs = parent.attributes

        assertNull("displayName attribute should not be used when bundle/key are provided", parentAttrs.getNamedItem("displayName"))

        val parentBundleAttr = parentAttrs.getNamedItem("bundle")
        val parentKeyAttr = parentAttrs.getNamedItem("key")
        assertNotNull("applicationConfigurable should declare bundle attribute", parentBundleAttr)
        assertNotNull("applicationConfigurable should declare key attribute", parentKeyAttr)
        assertEquals("messages.MyBundle", parentBundleAttr!!.nodeValue)
        assertEquals("settings.parent.title", parentKeyAttr!!.nodeValue)

        val projectConfigurables = doc.getElementsByTagName("projectConfigurable")
        assertTrue("plugin.xml must have a <projectConfigurable> entry", projectConfigurables.length > 0)
        val child = projectConfigurables.item(0)
        val childAttrs = child.attributes

        assertNull("displayName attribute should not be used when bundle/key are provided", childAttrs.getNamedItem("displayName"))

        val childBundleAttr = childAttrs.getNamedItem("bundle")
        val childKeyAttr = childAttrs.getNamedItem("key")
        assertNotNull("projectConfigurable should declare bundle attribute", childBundleAttr)
        assertNotNull("projectConfigurable should declare key attribute", childKeyAttr)
        assertEquals("messages.MyBundle", childBundleAttr!!.nodeValue)
        assertEquals("settings.child.title", childKeyAttr!!.nodeValue)
    }
}
