package com.github.lucsartes.intellijprojectidentifierplugin.adapters.intellij

import com.github.lucsartes.intellijprojectidentifierplugin.core.PluginSettings
import com.github.lucsartes.intellijprojectidentifierplugin.ports.SettingsPort
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.ui.components.JBCheckBox
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.*

/**
 * Settings UI for the plugin, appears under Appearance & Behavior.
 * Lives in adapters to keep IntelliJ UI dependencies out of core/ports.
 */
class IntelliJSettingsConfigurable : SearchableConfigurable {

    private val log = Logger.getInstance(IntelliJSettingsConfigurable::class.java)
    private val service: SettingsPort = ApplicationManager.getApplication().getService(SettingsPort::class.java)

    private var panel: JPanel? = null
    private lateinit var enabledCheckBox: JBCheckBox
    private lateinit var identifierField: JTextField

    override fun getId(): String = "com.github.lucsartes.intellijprojectidentifierplugin.settings"

    override fun getDisplayName(): String = "Project Identifier"

    override fun getPreferredFocusedComponent(): JComponent? = if (this::identifierField.isInitialized) identifierField else null

    private fun labeled(component: JComponent, labelText: String): JComponent {
        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            alignmentX = JComponent.LEFT_ALIGNMENT
            val label = JLabel(labelText)
            label.alignmentX = JComponent.LEFT_ALIGNMENT
            component.alignmentX = JComponent.LEFT_ALIGNMENT
            add(label)
            add(Box.createVerticalStrut(4))
            add(component)
        }
    }

    override fun createComponent(): JComponent {
        if (panel == null) {
            log.info("Creating Project Identifier settings UI panel")
            panel = JPanel(GridBagLayout()).apply {
                val gbc = GridBagConstraints().apply {
                    gridx = 0
                    gridy = 0
                    weightx = 1.0
                    fill = GridBagConstraints.HORIZONTAL
                    anchor = GridBagConstraints.NORTHWEST
                    insets = Insets(6, 6, 6, 6)
                }

                // Enabled checkbox
                enabledCheckBox = JBCheckBox("Enable project identifier watermark")
                add(enabledCheckBox, gbc)

                // Identifier override field with label
                gbc.gridy++
                identifierField = JTextField()
                add(labeled(identifierField, "Override identifier text (optional)"), gbc)

                gbc.gridy++
                gbc.weighty = 1.0
                add(Box.createVerticalGlue(), gbc)
            }

            // Initialize values
            reset()
        }
        return panel as JPanel
    }

    override fun isModified(): Boolean {
        val s = service.load()
        val uiEnabled = enabledCheckBox.isSelected
        val uiIdentifier = identifierField.text.ifBlank { null }
        val modified = (uiEnabled != s.enabled) ||
                (uiIdentifier != s.identifierOverride)
        log.info("Settings UI isModified: $modified (uiEnabled=$uiEnabled, uiIdentifier=$uiIdentifier) vs (enabled=${s.enabled}, identifier=${s.identifierOverride})")
        return modified
    }

    override fun apply() {
        val settings = PluginSettings(
            enabled = enabledCheckBox.isSelected,
            identifierOverride = identifierField.text.ifBlank { null }
        )
        log.info("Applying settings from UI: enabled=${settings.enabled}, override=${settings.identifierOverride}")
        service.save(settings)
    }

    override fun reset() {
        val s = service.load()
        log.info("Resetting settings UI from service: enabled=${s.enabled}, override=${s.identifierOverride}")
        enabledCheckBox.isSelected = s.enabled
        identifierField.text = s.identifierOverride ?: ""
    }

    override fun disposeUIResources() {
        log.info("Disposing settings UI resources")
        panel = null
    }
}
