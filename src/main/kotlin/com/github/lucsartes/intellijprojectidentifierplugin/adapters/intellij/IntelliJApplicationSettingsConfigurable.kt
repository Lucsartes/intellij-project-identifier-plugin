package com.github.lucsartes.intellijprojectidentifierplugin.adapters.intellij

import com.github.lucsartes.intellijprojectidentifierplugin.MyBundle
import com.github.lucsartes.intellijprojectidentifierplugin.core.ApplicationSettings
import com.github.lucsartes.intellijprojectidentifierplugin.ports.ApplicationSettingsPort
import com.intellij.icons.AllIcons
import com.intellij.ide.HelpTooltip
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.SearchableConfigurable
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.*

/**
 * Application-level settings configurable for Project Identifier plugin.
 * Provides UI for configuring global settings that apply to all projects.
 * This is also a parent configurable for the project-level settings.
 */
class IntelliJApplicationSettingsConfigurable : SearchableConfigurable, Configurable.Composite {

    private val log = Logger.getInstance(IntelliJApplicationSettingsConfigurable::class.java)

    // UI components
    private var panel: JPanel? = null
    private var ignoredWordsTextField: JTextField? = null
    private var resetButton: JButton? = null

    override fun getId(): String = "com.github.lucsartes.intellijprojectidentifierplugin.application.settings"

    override fun getDisplayName(): String = MyBundle.message("settings.parent.title")

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

    // Constrain the width of wide components so they don't span the whole settings page.
    private fun restrictWidth(component: JComponent, preferredWidth: Int = 300, minWidth: Int = 160) {
        val height = when {
            component.preferredSize.height > 0 -> component.preferredSize.height
            component.minimumSize.height > 0 -> component.minimumSize.height
            else -> 24
        }
        val width = preferredWidth.coerceAtLeast(minWidth)
        component.preferredSize = Dimension(width, height)
        component.maximumSize = Dimension(width, height)
        component.alignmentX = JComponent.LEFT_ALIGNMENT
    }

    override fun createComponent(): JComponent {
        log.info("Creating application settings UI component")

        val mainPanel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints().apply {
            gridx = 0
            gridy = 0
            weightx = 1.0
            fill = GridBagConstraints.HORIZONTAL
            anchor = GridBagConstraints.NORTHWEST
            insets = Insets(6, 6, 6, 6)
        }

        // Ignored words field with label and help icon
        gbc.gridy++
        ignoredWordsTextField = JTextField()
        restrictWidth(ignoredWordsTextField!!)
        val labelWithHelp = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            alignmentX = JComponent.LEFT_ALIGNMENT

            // Label row with help icon
            val labelRow = JPanel().apply {
                layout = BoxLayout(this, BoxLayout.X_AXIS)
                alignmentX = JComponent.LEFT_ALIGNMENT
                val label = JLabel(MyBundle.message("settings.application.ignored.words.label"))
                add(label)
                add(Box.createHorizontalStrut(6))
                val helpIcon = JLabel(AllIcons.General.ContextHelp)
                HelpTooltip()
                    .setDescription(MyBundle.message("settings.application.ignored.words.tooltip"))
                    .installOn(helpIcon)
                add(helpIcon)
            }
            add(labelRow)
            add(Box.createVerticalStrut(4))
            ignoredWordsTextField!!.alignmentX = JComponent.LEFT_ALIGNMENT
            add(ignoredWordsTextField!!)
        }
        mainPanel.add(labelWithHelp, gbc)

        // Reset button label
        gbc.gridy++
        val resetLabel = JLabel(MyBundle.message("settings.application.reset.label"))
        mainPanel.add(resetLabel, gbc)

        // Reset button - keep it compact, don't stretch to full width
        gbc.gridy++
        resetButton = JButton(MyBundle.message("settings.application.reset.button"))
        resetButton?.addActionListener {
            log.info("Reset button clicked for application settings")
            resetToDefaults()
        }
        // Temporarily adjust GridBag constraints to prevent stretching this button
        val oldFill = gbc.fill
        val oldWeightX = gbc.weightx
        val oldAnchor = gbc.anchor
        gbc.fill = GridBagConstraints.NONE
        gbc.weightx = 0.0
        gbc.anchor = GridBagConstraints.WEST
        mainPanel.add(resetButton!!, gbc)
        // Restore defaults for subsequent components
        gbc.fill = oldFill
        gbc.weightx = oldWeightX
        gbc.anchor = oldAnchor

        // Spacer to push everything up
        gbc.gridy++
        gbc.weighty = 1.0
        mainPanel.add(Box.createVerticalGlue(), gbc)

        panel = mainPanel
        return panel!!
    }

    override fun isModified(): Boolean {
        val service = ApplicationManager.getApplication().getService(ApplicationSettingsPort::class.java)
        val settings = service.load()
        return getIgnoredWordsFromUI() != settings.ignoredWords
    }

    override fun apply() {
        log.info("Applying application settings")
        val service = ApplicationManager.getApplication().getService(ApplicationSettingsPort::class.java)
        val newSettings = ApplicationSettings(
            ignoredWords = getIgnoredWordsFromUI()
        )
        service.save(newSettings)
        log.info("Application settings saved: ignoredWords=${newSettings.ignoredWords}")
    }

    override fun reset() {
        log.info("Resetting application settings UI to current values")
        val service = ApplicationManager.getApplication().getService(ApplicationSettingsPort::class.java)
        val current = service.load()

        ignoredWordsTextField?.text = current.ignoredWords.joinToString(", ")
        log.info("Reset application settings UI to: ignoredWords=${current.ignoredWords}")
    }

    private fun getIgnoredWordsFromUI(): Set<String> {
        return ignoredWordsTextField?.text?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.toSet() ?: emptySet()
    }

    private fun resetToDefaults() {
        val defaultSettings = ApplicationSettings()
        ignoredWordsTextField?.text = defaultSettings.ignoredWords.joinToString(", ")
        log.info("Reset application settings UI to default values")
    }

    override fun getConfigurables(): Array<Configurable> {
        // Children are automatically discovered from plugin.xml based on parentId
        return emptyArray()
    }

    override fun disposeUIResources() {
        panel = null
        ignoredWordsTextField = null
        resetButton = null
    }
}
