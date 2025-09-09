package com.github.lucsartes.intellijprojectidentifierplugin.adapters.intellij

import com.github.lucsartes.intellijprojectidentifierplugin.ports.IdentifierService
import com.github.lucsartes.intellijprojectidentifierplugin.ports.ImageService
import com.intellij.openapi.application.ApplicationManager
import com.intellij.testFramework.ApplicationRule
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test

/**
 * Integration test to verify that services are correctly wired via plugin.xml
 * and retrievable from the IntelliJ Platform service manager.
 */
class ServiceWiringIntegrationTest {

    @Rule
    @JvmField
    val appRule = ApplicationRule()

    @Test
    fun servicesAreRegistered() {
        val app = ApplicationManager.getApplication()

        val identifierService = app.getService(IdentifierService::class.java)
        val imageService = app.getService(ImageService::class.java)

        assertNotNull("IdentifierService should be registered as application service", identifierService)
        assertNotNull("ImageService should be registered as application service", imageService)
    }
}
