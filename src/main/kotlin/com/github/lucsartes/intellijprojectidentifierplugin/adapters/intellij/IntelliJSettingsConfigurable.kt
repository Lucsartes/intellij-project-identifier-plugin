package com.github.lucsartes.intellijprojectidentifierplugin.adapters.intellij

import com.github.lucsartes.intellijprojectidentifierplugin.core.PluginSettings
import com.github.lucsartes.intellijprojectidentifierplugin.ports.SettingsPort
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBCheckBox
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.GraphicsEnvironment
import java.awt.Insets
import javax.swing.*

/**
 * Settings UI for the plugin, appears under Appearance & Behavior.
 * Lives in adapters to keep IntelliJ UI dependencies out of core/ports.
 */
class IntelliJSettingsConfigurable(private val project: Project) : SearchableConfigurable {

    private val log = Logger.getInstance(IntelliJSettingsConfigurable::class.java)
    private val service: SettingsPort by lazy { project.getService(SettingsPort::class.java) }

    private var panel: JPanel? = null
    private lateinit var enabledCheckBox: JBCheckBox
    private lateinit var identifierField: JTextField
    private lateinit var fontCombo: JComboBox<String>
    private lateinit var sizeCombo: JComboBox<String>

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

                // Header explaining scope
                val scopeLabel = JLabel("These settings apply only to this project.")
                add(scopeLabel, gbc)

                // Enabled checkbox
                gbc.gridy++
                enabledCheckBox = JBCheckBox("Enable project identifier watermark")
                add(enabledCheckBox, gbc)

                // Identifier override field with label
                gbc.gridy++
                identifierField = JTextField()
                add(labeled(identifierField, "Override identifier text (optional)"), gbc)

                // Font family dropdown
                gbc.gridy++
                run {
                    val fontNames = try {
                        GraphicsEnvironment.getLocalGraphicsEnvironment().availableFontFamilyNames.toList().sorted()
                    } catch (t: Throwable) {
                        emptyList<String>()
                    }
                    val model = DefaultComboBoxModel<String>()
                    model.addElement("(Default)")
                    fontNames.forEach { model.addElement(it) }
                    fontCombo = JComboBox(model)
                    add(labeled(fontCombo, "Font family"), gbc)
                }

                // Text size dropdown (pixels)
                gbc.gridy++
                run {
                    val model = DefaultComboBoxModel<String>()
                    model.addElement("(Default)")
                    val sizes = listOf(8, 10, 12, 14, 16, 18, 20, 24, 28, 32, 36, 40, 48, 56, 64, 72)
                    sizes.forEach { model.addElement(it.toString()) }
                    sizeCombo = JComboBox(model)
                    add(labeled(sizeCombo, "Text size (px)"), gbc)
                }

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
        val uiFont = (if (this::fontCombo.isInitialized) fontCombo.selectedItem as? String else null)?.let { if (it == "(Default)") null else it }
        val uiSizePx = (if (this::sizeCombo.isInitialized) sizeCombo.selectedItem as? String else null)?.toIntOrNull()
        val modified = (uiEnabled != s.enabled) ||
                (uiIdentifier != s.identifierOverride) ||
                (uiFont != s.fontFamily) ||
                (uiSizePx != s.fontSizePx)
        log.info("Settings UI isModified: $modified (uiEnabled=$uiEnabled, uiIdentifier=$uiIdentifier, uiFont=$uiFont, uiSizePx=$uiSizePx) vs (enabled=${s.enabled}, identifier=${s.identifierOverride}, fontFamily=${s.fontFamily}, fontSizePx=${s.fontSizePx})")
        return modified
    }

    override fun apply() {
        val uiFont = (fontCombo.selectedItem as? String)?.let { if (it == "(Default)") null else it }
        val uiSizePx = (sizeCombo.selectedItem as? String)?.toIntOrNull()
        val settings = PluginSettings(
            enabled = enabledCheckBox.isSelected,
            identifierOverride = identifierField.text.ifBlank { null },
            fontFamily = uiFont,
            fontSizePx = uiSizePx
        )
        log.info("Applying settings from UI: enabled=${settings.enabled}, override=${settings.identifierOverride}, fontFamily=${settings.fontFamily}, fontSizePx=${settings.fontSizePx}")
        service.save(settings)
    }

    override fun reset() {
        val s = service.load()
        log.info("Resetting settings UI from service: enabled=${s.enabled}, override=${s.identifierOverride}, fontFamily=${s.fontFamily}, fontSizePx=${s.fontSizePx}")
        enabledCheckBox.isSelected = s.enabled
        identifierField.text = s.identifierOverride ?: ""
        // Font selection
        run {
            val desired = s.fontFamily?.ifBlank { null }
            val model = fontCombo.model
            val count = model.size
            var found = false
            for (i in 0 until count) {
                val v = model.getElementAt(i)
                if (desired == null && v == "(Default)" || desired != null && v == desired) {
                    fontCombo.selectedIndex = i
                    found = true
                    break
                }
            }
            if (!found) {
                // if desired font isn't available, fall back to Default
                fontCombo.selectedIndex = 0
            }
        }
        // Size selection
        run {
            val desired = s.fontSizePx
            val model = sizeCombo.model
            val asString = desired?.toString() ?: "(Default)"
            val count = model.size
            var found = false
            for (i in 0 until count) {
                val v = model.getElementAt(i)
                if (v == asString) {
                    sizeCombo.selectedIndex = i
                    found = true
                    break
                }
            }
            if (!found) {
                // if custom size isn't present, add it and select
                if (desired != null) {
                    (model as DefaultComboBoxModel<String>).addElement(asString)
                    sizeCombo.selectedItem = asString
                } else {
                    sizeCombo.selectedIndex = 0
                }
            }
        }
    }

    override fun disposeUIResources() {
        log.info("Disposing settings UI resources")
        panel = null
    }
}
