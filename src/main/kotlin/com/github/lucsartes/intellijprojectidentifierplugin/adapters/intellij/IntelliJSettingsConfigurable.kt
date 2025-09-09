package com.github.lucsartes.intellijprojectidentifierplugin.adapters.intellij

import com.github.lucsartes.intellijprojectidentifierplugin.core.PluginSettings
import com.github.lucsartes.intellijprojectidentifierplugin.ports.SettingsPort
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.SearchableConfigurable
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.*

/**
 * Settings UI for the plugin, appears under Appearance & Behavior.
 * Lives in adapters to keep IntelliJ UI dependencies out of core/ports.
 */
class IntelliJSettingsConfigurable : SearchableConfigurable {

    private val service: SettingsPort = ApplicationManager.getApplication().getService(SettingsPort::class.java)

    private var panel: JPanel? = null
    private lateinit var enabledCheckBox: JCheckBox
    private lateinit var opacitySlider: JSlider
    private lateinit var opacityValueLabel: JLabel
    private lateinit var identifierField: JTextField

    override fun getId(): String = "com.github.lucsartes.intellijprojectidentifierplugin.settings"

    override fun getDisplayName(): String = "Project Identifier"

    override fun getPreferredFocusedComponent(): JComponent? = if (this::identifierField.isInitialized) identifierField else null

    override fun createComponent(): JComponent {
        if (panel == null) {
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
                enabledCheckBox = JCheckBox("Enable project identifier watermark")
                add(enabledCheckBox, gbc)

                // Opacity label and slider
                gbc.gridy++
                add(JLabel("Watermark opacity"), gbc)

                gbc.gridy++
                val sliderPanel = JPanel().apply {
                    layout = BoxLayout(this, BoxLayout.X_AXIS)
                    opacitySlider = JSlider(0, 100)
                    opacitySlider.majorTickSpacing = 25
                    opacitySlider.minorTickSpacing = 5
                    opacitySlider.paintTicks = true
                    opacitySlider.paintLabels = false
                    opacityValueLabel = JLabel("")
                    add(opacitySlider)
                    add(Box.createHorizontalStrut(8))
                    add(opacityValueLabel)
                }
                add(sliderPanel, gbc)

                // Identifier override
                gbc.gridy++
                add(JLabel("Override identifier text (optional)"), gbc)

                gbc.gridy++
                identifierField = JTextField()
                add(identifierField, gbc)

                gbc.gridy++
                gbc.weighty = 1.0
                add(Box.createVerticalGlue(), gbc)
            }

            // Initialize values and listeners
            reset()
            opacitySlider.addChangeListener {
                opacityValueLabel.text = "${'$'}{opacitySlider.value}%"
            }
        }
        return panel as JPanel
    }

    override fun isModified(): Boolean {
        val s = service.load()
        val uiEnabled = enabledCheckBox.isSelected
        val uiOpacity = opacitySlider.value / 100.0f
        val uiIdentifier = identifierField.text.ifBlank { null }
        return (uiEnabled != s.enabled) ||
                (kotlin.math.abs(uiOpacity - s.opacity) > 0.001f) ||
                (uiIdentifier != s.identifierOverride)
    }

    override fun apply() {
        val settings = PluginSettings(
            enabled = enabledCheckBox.isSelected,
            opacity = (opacitySlider.value / 100.0f).coerceIn(0.0f, 1.0f),
            identifierOverride = identifierField.text.ifBlank { null }
        )
        service.save(settings)
    }

    override fun reset() {
        val s = service.load()
        enabledCheckBox.isSelected = s.enabled
        opacitySlider.value = (s.opacity * 100).toInt().coerceIn(0, 100)
        opacityValueLabel.text = "${'$'}{opacitySlider.value}%"
        identifierField.text = s.identifierOverride ?: ""
    }

    override fun disposeUIResources() {
        panel = null
    }
}
