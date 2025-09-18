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

    // Cached defaults used for rendering labels like "(Default)"
    private var defaultFontFamily: String? = null
    private val defaultFontSizePx: Int = 144

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
                    // We intentionally show a curated list of popular, ASCII-named fonts instead of
                    // every installed family. Many systems expose hundreds of families whose names or
                    // glyph coverage render as tofu (small rectangles) in the dropdown, making the UI hard
                    // to read. Starting simple avoids that UX issue while we evaluate a richer picker.
                    // If a selected font is not installed on this machine, rendering will gracefully fall
                    // back to SansSerif in the core (see ImageServiceImpl). We also filter this list by
                    // what the JRE reports as available to keep it relevant on the current OS.
                    val curated = listOf(
                        "Arial","Helvetica","Times New Roman","Times","Courier New","Courier","Verdana","Tahoma","Trebuchet MS","Georgia",
                        "Palatino","Garamond","Bookman","Comic Sans MS","Candara","Calibri","Cambria","Constantia","Consolas","Lucida Console",
                        "Lucida Sans","Lucida Sans Unicode","Segoe UI","Segoe UI Emoji","Menlo","Monaco","Avenir","Avenir Next","Optima","Gill Sans",
                        "Franklin Gothic Medium","Century Gothic","Baskerville","Didot","Futura","Rockwell","Goudy Old Style","Copperplate","DejaVu Sans","DejaVu Serif",
                        "DejaVu Sans Mono","Liberation Sans","Liberation Serif","Liberation Mono","Noto Sans","Noto Serif","Noto Sans Mono","Ubuntu","Ubuntu Mono","JetBrains Mono"
                    )
                    val available = try {
                        GraphicsEnvironment.getLocalGraphicsEnvironment().availableFontFamilyNames.toSet()
                    } catch (t: Throwable) {
                        emptySet<String>()
                    }
                    val fontNames = if (available.isEmpty()) curated else curated.filter { it in available }
                    // Determine the default font used by the app pipeline when none is selected.
                    defaultFontFamily = fontNames.firstOrNull { it == "JetBrains Mono" }
                    val model = DefaultComboBoxModel<String>()
                    fontNames.forEach { model.addElement(it) }
                    fontCombo = JComboBox(model)
                    // Render each entry using its own font and annotate the default choice.
                    fontCombo.renderer = object : DefaultListCellRenderer() {
                        override fun getListCellRendererComponent(list: JList<*>, value: Any?, index: Int, isSelected: Boolean, cellHasFocus: Boolean): java.awt.Component {
                            val c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus) as JLabel
                            val name = value as? String
                            if (name != null) {
                                val isDefault = (defaultFontFamily != null && name == defaultFontFamily)
                                c.text = if (isDefault) "$name (Default)" else name
                                try {
                                    c.font = c.font.deriveFont(java.awt.Font.PLAIN).let { base -> java.awt.Font(name, base.style, base.size) }
                                } catch (_: Throwable) {
                                    // If font instantiation fails, keep default label font.
                                }
                            }
                            return c
                        }
                    }
                    add(labeled(fontCombo, "Font family"), gbc)
                }

                // Text size dropdown (pixels)
                gbc.gridy++
                run {
                    val model = DefaultComboBoxModel<String>()
                    val sizes = listOf(72, 80, 96, 112, 128, 144, 160, 192, 224, 256)
                    sizes.forEach { model.addElement(it.toString()) }
                    sizeCombo = JComboBox(model)
                    // Renderer that appends (Default) to the 144px entry
                    sizeCombo.renderer = object : DefaultListCellRenderer() {
                        override fun getListCellRendererComponent(list: JList<*>, value: Any?, index: Int, isSelected: Boolean, cellHasFocus: Boolean): java.awt.Component {
                            val c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus) as JLabel
                            val txt = (value as? String)
                            if (txt != null) {
                                val isDefault = txt.toIntOrNull() == defaultFontSizePx
                                c.text = if (isDefault) "$txt (Default)" else txt
                            }
                            return c
                        }
                    }
                    add(labeled(sizeCombo, "Text size (px)"), gbc)
                }


                // Hint about position/opacity settings location
                gbc.gridy++
                val hintLabel = JLabel("To change the watermark position and opacity, go to Appearance & Behavior | Appearance | Background Image.")
                add(hintLabel, gbc)

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
        val selectedFont = (if (this::fontCombo.isInitialized) fontCombo.selectedItem as? String else null)
        val uiFont = selectedFont?.let { if (defaultFontFamily != null && it == defaultFontFamily) null else it }
        val selectedSizeStr = (if (this::sizeCombo.isInitialized) sizeCombo.selectedItem as? String else null)
        val selectedSize = selectedSizeStr?.toIntOrNull()
        val uiSizePx = selectedSize?.let { if (it == defaultFontSizePx) null else it }
        val modified = (uiEnabled != s.enabled) ||
                (uiIdentifier != s.identifierOverride) ||
                (uiFont != s.fontFamily) ||
                (uiSizePx != s.fontSizePx)
        log.info("Settings UI isModified: $modified (uiEnabled=$uiEnabled, uiIdentifier=$uiIdentifier, uiFont=$uiFont, uiSizePx=$uiSizePx) vs (enabled=${s.enabled}, identifier=${s.identifierOverride}, fontFamily=${s.fontFamily}, fontSizePx=${s.fontSizePx})")
        return modified
    }

    override fun apply() {
        val selectedFont = (fontCombo.selectedItem as? String)
        val uiFont = selectedFont?.let { if (defaultFontFamily != null && it == defaultFontFamily) null else it }
        val selectedSize = (sizeCombo.selectedItem as? String)?.toIntOrNull()
        val uiSizePx = selectedSize?.let { if (it == defaultFontSizePx) null else it }
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
                if (desired != null && v == desired) {
                    fontCombo.selectedIndex = i
                    found = true
                    break
                }
            }
            if (!found) {
                // If no explicit font set, select the computed default if present, otherwise first item.
                val fallback = defaultFontFamily
                if (fallback != null) {
                    for (i in 0 until count) {
                        if (model.getElementAt(i) == fallback) {
                            fontCombo.selectedIndex = i
                            found = true
                            break
                        }
                    }
                }
                if (!found) {
                    fontCombo.selectedIndex = 0
                }
            }
        }
        // Size selection
        run {
            val desired = s.fontSizePx
            val model = sizeCombo.model
            val asString = (desired ?: defaultFontSizePx).toString()
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
                    // should not happen as default size is in the list
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
