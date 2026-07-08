package com.github.lucsartes.intellijprojectidentifierplugin.adapters.intellij

import java.awt.Dimension
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

/**
 * Small shared Swing helpers for the plugin's settings pages, used by both the project-level and
 * application-level configurables so the two stay visually consistent.
 */
internal object SettingsUiSupport {

    /** Stacks [labelText] above [component] in a left-aligned vertical group. */
    fun labeled(component: JComponent, labelText: String): JComponent =
        JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            alignmentX = JComponent.LEFT_ALIGNMENT
            val label = JLabel(labelText)
            label.alignmentX = JComponent.LEFT_ALIGNMENT
            component.alignmentX = JComponent.LEFT_ALIGNMENT
            add(label)
            add(Box.createVerticalStrut(4))
            add(component)
        }

    /** Constrains [component]'s width so it doesn't span the whole settings page. */
    fun restrictWidth(component: JComponent, preferredWidth: Int = 300, minWidth: Int = 160) {
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
}
