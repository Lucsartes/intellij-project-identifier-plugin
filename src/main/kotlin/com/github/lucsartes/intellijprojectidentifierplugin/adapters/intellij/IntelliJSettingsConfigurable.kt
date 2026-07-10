package com.github.lucsartes.intellijprojectidentifierplugin.adapters.intellij

import com.github.lucsartes.intellijprojectidentifierplugin.core.FontSupport
import com.github.lucsartes.intellijprojectidentifierplugin.core.IdentifierGenerator
import com.github.lucsartes.intellijprojectidentifierplugin.core.ImageRenderer
import com.github.lucsartes.intellijprojectidentifierplugin.core.ProjectSettings
import com.github.lucsartes.intellijprojectidentifierplugin.core.TemplateResolver
import com.github.lucsartes.intellijprojectidentifierplugin.ports.ApplicationSettingsPort
import com.github.lucsartes.intellijprojectidentifierplugin.ports.BackgroundImagePort
import com.github.lucsartes.intellijprojectidentifierplugin.ports.BranchProvider
import com.github.lucsartes.intellijprojectidentifierplugin.ports.ProjectSettingsPort
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.Project
import com.intellij.ui.ColorPanel
import com.intellij.util.ui.UIUtil
import com.intellij.ide.HelpTooltip
import com.intellij.icons.AllIcons
import com.github.lucsartes.intellijprojectidentifierplugin.MyBundle
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.GraphicsEnvironment
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

/**
 * Settings UI for the plugin, appears under Appearance & Behavior.
 * Lives in adapters to keep IntelliJ UI dependencies out of core/ports.
 */
class IntelliJSettingsConfigurable(private val project: Project) : SearchableConfigurable {

    private val log = Logger.getInstance(IntelliJSettingsConfigurable::class.java)
    private val service: ProjectSettingsPort by lazy { project.getService(ProjectSettingsPort::class.java) }

    private var panel: JPanel? = null
    private lateinit var identifierField: JTextField
    private lateinit var fontCombo: JComboBox<String>
    private lateinit var sizeCombo: JComboBox<String>
    private lateinit var colorPanel: ColorPanel
    private lateinit var previewPanel: PreviewPanel

    // Pure-core collaborators reused to render the live preview exactly as the watermark pipeline would.
    private val identifierGenerator = IdentifierGenerator()
    private val imageRenderer = ImageRenderer()
    private val templateResolver = TemplateResolver()

    // The branch is resolved once per dialog session (it rarely changes while Settings is open).
    private var cachedBranch: String? = null
    private var branchResolved = false

    // Guards the preview from re-rendering repeatedly while reset() bulk-updates the controls.
    private var suppressPreview = false

    // Cached defaults used for rendering labels like "(Default)"
    private var defaultFontFamily: String? = null
    private val defaultFontSizePx: Int = com.github.lucsartes.intellijprojectidentifierplugin.core.CoreDefaults.DEFAULT_FONT_SIZE_PX

    override fun getId(): String = "com.github.lucsartes.intellijprojectidentifierplugin.settings"

    override fun getDisplayName(): String = MyBundle.message("settings.child.title")

    override fun getPreferredFocusedComponent(): JComponent? = if (this::identifierField.isInitialized) identifierField else null

    override fun createComponent(): JComponent {
        if (panel == null) {
            log.info("Creating Project Identifier settings UI panel")
            // Left column: the narrow content controls plus the reset action (they don't need the full width).
            val controlsColumn = JPanel(GridBagLayout()).apply {
                val gbc = GridBagConstraints().apply {
                    gridx = 0
                    gridy = 0
                    weightx = 1.0
                    fill = GridBagConstraints.HORIZONTAL
                    anchor = GridBagConstraints.NORTHWEST
                    insets = Insets(6, 6, 6, 6)
                }


                // Identifier override field with label and a help icon documenting placeholders (e.g. ${branch})
                gbc.gridy++
                identifierField = JTextField()
                SettingsUiSupport.restrictWidth(identifierField)
                val overridePanel = JPanel().apply {
                    layout = BoxLayout(this, BoxLayout.Y_AXIS)
                    alignmentX = JComponent.LEFT_ALIGNMENT
                    val labelRow = JPanel().apply {
                        layout = BoxLayout(this, BoxLayout.X_AXIS)
                        alignmentX = JComponent.LEFT_ALIGNMENT
                        add(JLabel(MyBundle.message("settings.identifier.override.label")))
                        add(Box.createHorizontalStrut(6))
                        val helpIcon = JLabel(AllIcons.General.ContextHelp)
                        HelpTooltip()
                            .setTitle(MyBundle.message("settings.identifier.override.tooltip.title"))
                            .setDescription(MyBundle.message("settings.identifier.override.tooltip.description"))
                            .installOn(helpIcon)
                        add(helpIcon)
                    }
                    add(labelRow)
                    add(Box.createVerticalStrut(4))
                    identifierField.alignmentX = JComponent.LEFT_ALIGNMENT
                    add(identifierField)
                }
                add(overridePanel, gbc)

                // Font family dropdown
                gbc.gridy++
                run {
                    // We intentionally show a curated list of popular, ASCII-named fonts instead of
                    // every installed family. Many systems expose hundreds of families whose names or
                    // glyph coverage render as tofu (small rectangles) in the dropdown, making the UI hard
                    // to read. Starting simple avoids that UX issue while we evaluate a richer picker.
                    // If a selected font is not installed on this machine, rendering will gracefully fall
                    // back to SansSerif in the core (see ImageRenderer). We also filter this list by
                    // what the JRE reports as available to keep it relevant on the current OS.
                    val curated = listOf(
                        "Arial", "Helvetica", "Times New Roman", "Times", "Courier New", "Courier", "Verdana", "Tahoma", "Trebuchet MS", "Georgia",
                        "Palatino", "Garamond", "Bookman", "Comic Sans MS", "Candara", "Calibri", "Cambria", "Constantia", "Consolas", "Lucida Console",
                        "Lucida Sans", "Lucida Sans Unicode", "Segoe UI", "Segoe UI Emoji", "Menlo", "Monaco", "Avenir", "Avenir Next", "Optima", "Gill Sans",
                        "Franklin Gothic Medium", "Century Gothic", "Baskerville", "Didot", "Futura", "Rockwell", "Goudy Old Style", "Copperplate", "DejaVu Sans", "DejaVu Serif",
                        "DejaVu Sans Mono", "Liberation Sans", "Liberation Serif", "Liberation Mono", "Noto Sans", "Noto Serif", "Noto Sans Mono", "Ubuntu", "Ubuntu Mono", "JetBrains Mono"
                    )
                    val available = try {
                        GraphicsEnvironment.getLocalGraphicsEnvironment().availableFontFamilyNames.toSet()
                    } catch (t: Throwable) {
                        emptySet<String>()
                    }
                    val fontNames = if (available.isEmpty()) curated else curated.filter { it in available }
                    // Determine the default font used by the app pipeline when none is selected.
                    defaultFontFamily = fontNames.firstOrNull { it == FontSupport.PREFERRED_DEFAULT }
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
                                c.text = if (isDefault) MyBundle.message("default.annotated.value", name) else name
                                try {
                                    c.font = c.font.deriveFont(java.awt.Font.PLAIN).let { base -> java.awt.Font(name, base.style, base.size) }
                                } catch (_: Throwable) {
                                    // If font instantiation fails, keep default label font.
                                }
                            }
                            return c
                        }
                    }
                    SettingsUiSupport.restrictWidth(fontCombo)
                    add(SettingsUiSupport.labeled(fontCombo, MyBundle.message("settings.font.family.label")), gbc)
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
                                c.text = if (isDefault) MyBundle.message("default.annotated.value", txt) else txt
                            }
                            return c
                        }
                    }
                    SettingsUiSupport.restrictWidth(sizeCombo)
                    add(SettingsUiSupport.labeled(sizeCombo, MyBundle.message("settings.text.size.px.label")), gbc)
                }


                // Text color picker
                gbc.gridy++
                colorPanel = ColorPanel()
                colorPanel.selectedColor = Color.WHITE
                SettingsUiSupport.restrictWidth(colorPanel)
                add(SettingsUiSupport.labeled(colorPanel, MyBundle.message("settings.text.color.label")), gbc)

                // Reset to defaults label and button
                gbc.gridy++
                val resetLabel = JLabel(MyBundle.message("settings.reset.label"))
                add(resetLabel, gbc)

                gbc.gridy++
                val resetButton = JButton(MyBundle.message("settings.reset.button"))
                // Keep the button compact; don't stretch it to full width
                resetButton.addActionListener {
                    runCatching {
                        // Reset IDE background menu settings to defaults first
                        val bg = project.getService(BackgroundImagePort::class.java)
                        bg.resetBackgroundSettingsToDefaults()
                    }.onFailure { t ->
                        log.warn("Failed to reset background settings to defaults", t)
                    }
                    // Reset plugin settings to defaults and refresh UI
                    val defaults = ProjectSettings()
                    log.info("Resetting plugin settings to defaults via UI action: override=${defaults.identifierOverride}, fontFamily=${defaults.fontFamily}, fontSizePx=${defaults.fontSizePx}, textColorArgb=${defaults.textColorArgb}")
                    service.save(defaults)
                    reset()
                }
                // Temporarily adjust GridBag constraints to prevent stretching this button
                val oldFill = gbc.fill
                val oldWeightX = gbc.weightx
                val oldAnchor = gbc.anchor
                gbc.fill = GridBagConstraints.NONE
                gbc.weightx = 0.0
                gbc.anchor = GridBagConstraints.WEST
                add(resetButton, gbc)
                // Restore defaults for subsequent components
                gbc.fill = oldFill
                gbc.weightx = oldWeightX
                gbc.anchor = oldAnchor
            }

            // Live preview, placed to the RIGHT of the controls. Content only: full opacity, over the current
            // IDE background color; on-screen opacity/position stay IDE-controlled. Lets the user compare
            // options without applying (and Cancel to discard).
            previewPanel = PreviewPanel()
            val previewSide = JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                val labelRow = JPanel().apply {
                    layout = BoxLayout(this, BoxLayout.X_AXIS)
                    alignmentX = JComponent.LEFT_ALIGNMENT
                    add(JLabel(MyBundle.message("settings.preview.label")))
                    add(Box.createHorizontalStrut(6))
                    val helpIcon = JLabel(AllIcons.General.ContextHelp)
                    HelpTooltip()
                        .setTitle(MyBundle.message("settings.preview.tooltip.title"))
                        .setDescription(MyBundle.message("settings.preview.tooltip.description"))
                        .installOn(helpIcon)
                    add(helpIcon)
                }
                add(labelRow)
                add(Box.createVerticalStrut(4))
                previewPanel.alignmentX = JComponent.LEFT_ALIGNMENT
                add(previewPanel)
            }

            // Top row: controls on the left, preview immediately to their right; a filler eats the rest of the width.
            val topRow = JPanel(GridBagLayout()).apply {
                val c = GridBagConstraints().apply {
                    gridx = 0
                    gridy = 0
                    anchor = GridBagConstraints.NORTHWEST
                    fill = GridBagConstraints.NONE
                    weightx = 0.0
                }
                add(controlsColumn, c)
                c.gridx = 1
                c.insets = Insets(6, 18, 6, 6)
                add(previewSide, c)
                c.gridx = 2
                c.insets = Insets(0, 0, 0, 0)
                c.weightx = 1.0
                c.fill = GridBagConstraints.HORIZONTAL
                add(Box.createHorizontalGlue(), c)
            }

            // Assemble: the two-column top row, then the full-width hint, then a vertical spacer.
            panel = JPanel(GridBagLayout()).apply {
                val gbc = GridBagConstraints().apply {
                    gridx = 0
                    gridy = 0
                    weightx = 1.0
                    fill = GridBagConstraints.HORIZONTAL
                    anchor = GridBagConstraints.NORTHWEST
                    insets = Insets(6, 6, 6, 6)
                }
                add(topRow, gbc)

                // Hint about position/opacity settings location (full width, below the controls)
                gbc.gridy++
                gbc.insets = Insets(18, 6, 6, 6)
                val hintLabel = JLabel(MyBundle.message("settings.hint.label"))
                val hintPanel = JPanel().apply {
                    layout = BoxLayout(this, BoxLayout.X_AXIS)
                    alignmentX = JComponent.LEFT_ALIGNMENT
                    add(hintLabel)
                    add(Box.createHorizontalStrut(6))
                    val helpIcon = JLabel(AllIcons.General.ContextHelp)
                    HelpTooltip()
                        .setTitle(MyBundle.message("settings.hint.tooltip.title"))
                        .setDescription(MyBundle.message("settings.hint.tooltip.description"))
                        .installOn(helpIcon)
                    add(helpIcon)
                }
                add(hintPanel, gbc)

                gbc.gridy++
                gbc.insets = Insets(6, 6, 6, 6)
                gbc.weighty = 1.0
                add(Box.createVerticalGlue(), gbc)
            }

            wirePreviewListeners()
            // Initialize values
            reset()
        }
        return panel as JPanel
    }

    override fun isModified(): Boolean {
        val s = service.load()
        val uiIdentifier = identifierField.text.ifBlank { null }
        val selectedFont = (if (this::fontCombo.isInitialized) fontCombo.selectedItem as? String else null)
        val uiFont = selectedFont?.let { if (defaultFontFamily != null && it == defaultFontFamily) null else it }
        val selectedSizeStr = (if (this::sizeCombo.isInitialized) sizeCombo.selectedItem as? String else null)
        val selectedSize = selectedSizeStr?.toIntOrNull()
        val uiSizePx = selectedSize?.let { if (it == defaultFontSizePx) null else it }
        val uiColorArgbRaw = if (this::colorPanel.isInitialized) colorPanel.selectedColor?.rgb else null
        val uiColorArgb = uiColorArgbRaw?.let { if (it == Color.WHITE.rgb) null else it }
        val modified = (uiIdentifier != s.identifierOverride) ||
                (uiFont != s.fontFamily) ||
                (uiSizePx != s.fontSizePx) ||
                (uiColorArgb != s.textColorArgb)
        log.info("Settings UI isModified: $modified (uiIdentifier=$uiIdentifier, uiFont=$uiFont, uiSizePx=$uiSizePx, uiColorArgb=$uiColorArgb) vs (identifier=${s.identifierOverride}, fontFamily=${s.fontFamily}, fontSizePx=${s.fontSizePx}, textColorArgb=${s.textColorArgb})")
        return modified
    }

    override fun apply() {
        val selectedFont = (fontCombo.selectedItem as? String)
        val uiFont = selectedFont?.let { if (defaultFontFamily != null && it == defaultFontFamily) null else it }
        val selectedSize = (sizeCombo.selectedItem as? String)?.toIntOrNull()
        val uiSizePx = selectedSize?.let { if (it == defaultFontSizePx) null else it }
        val uiColorArgbRaw = colorPanel.selectedColor?.rgb
        val uiColorArgb = uiColorArgbRaw?.let { if (it == Color.WHITE.rgb) null else it }
        val settings = ProjectSettings(
            identifierOverride = identifierField.text.ifBlank { null },
            fontFamily = uiFont,
            fontSizePx = uiSizePx,
            textColorArgb = uiColorArgb
        )
        log.info("Applying settings from UI: override=${settings.identifierOverride}, fontFamily=${settings.fontFamily}, fontSizePx=${settings.fontSizePx}, textColorArgb=${settings.textColorArgb}")
        service.save(settings)
    }

    override fun reset() {
        val s = service.load()
        log.info("Resetting settings UI from service: identifier=${s.identifierOverride}, fontFamily=${s.fontFamily}, fontSizePx=${s.fontSizePx}, textColorArgb=${s.textColorArgb}")
        suppressPreview = true
        identifierField.text = s.identifierOverride ?: ""
        colorPanel.selectedColor = (s.textColorArgb?.let { Color(it, true) } ?: Color.WHITE)
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
        suppressPreview = false
        updatePreview()
    }

    override fun disposeUIResources() {
        log.info("Disposing settings UI resources")
        panel = null
        branchResolved = false
        cachedBranch = null
    }

    private fun wirePreviewListeners() {
        identifierField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) = updatePreview()
            override fun removeUpdate(e: DocumentEvent) = updatePreview()
            override fun changedUpdate(e: DocumentEvent) = updatePreview()
        })
        fontCombo.addActionListener { updatePreview() }
        sizeCombo.addActionListener { updatePreview() }
        colorPanel.addActionListener { updatePreview() }
    }

    /** Re-renders the preview from the current UI control values, mirroring what the watermark pipeline would draw. */
    private fun updatePreview() {
        if (suppressPreview || !this::previewPanel.isInitialized) return
        val image = runCatching {
            val selectedFont = fontCombo.selectedItem as? String
            val uiFont = selectedFont?.let { if (defaultFontFamily != null && it == defaultFontFamily) null else it }
            val uiSizePx = (sizeCombo.selectedItem as? String)?.toIntOrNull()?.let { if (it == defaultFontSizePx) null else it }
            val uiColorArgb = colorPanel.selectedColor?.rgb?.let { if (it == Color.WHITE.rgb) null else it }
            val text = resolvePreviewText()
            if (text.isBlank()) null else ImageIO.read(ByteArrayInputStream(imageRenderer.renderPng(text, uiFont, uiSizePx, uiColorArgb)))
        }.onFailure { log.debug("Failed to render settings preview", it) }.getOrNull()
        previewPanel.image = image
    }

    /** Resolves the effective watermark text like the pipeline does: override or derived name, with ${branch} filled in. */
    private fun resolvePreviewText(): String {
        val base = identifierField.text.ifBlank { null } ?: identifierGenerator.generate(project.name, ignoredWords())
        val branch = if (templateResolver.usesPlaceholder(base, TemplateResolver.BRANCH)) previewBranch() else null
        return templateResolver.resolve(base, mapOf(TemplateResolver.BRANCH to branch))
    }

    private fun ignoredWords(): Set<String> =
        runCatching {
            ApplicationManager.getApplication().getService(ApplicationSettingsPort::class.java).load().ignoredWords.toSet()
        }.getOrDefault(emptySet())

    private fun previewBranch(): String? {
        if (!branchResolved) {
            cachedBranch = runCatching { project.getService(BranchProvider::class.java).currentBranch() }.getOrNull()
            branchResolved = true
        }
        return cachedBranch
    }

    /** The IDE background color the watermark sits on, so white-on-white (or dark-on-dark) never hides the text. */
    private fun previewBackgroundColor(): Color =
        runCatching { UIUtil.getPanelBackground() }.getOrNull()
            ?: Color(0x2B, 0x2B, 0x2B)

    /**
     * Small fixed-size panel that paints the rendered watermark image scaled to fit, on top of the current editor
     * background color — so the preview reflects how the chosen text/font/size/color will actually read. Content only:
     * on-screen opacity and position stay controlled by the IDE Background Image page.
     */
    private inner class PreviewPanel : JPanel() {
        var image: BufferedImage? = null
            set(value) {
                field = value
                repaint()
            }

        init {
            val size = Dimension(PREVIEW_WIDTH_PX, PREVIEW_HEIGHT_PX)
            preferredSize = size
            minimumSize = size
            maximumSize = size
            alignmentX = JComponent.LEFT_ALIGNMENT
            border = BorderFactory.createLineBorder(Color.GRAY)
            isOpaque = true
        }

        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)
            val g2 = g.create() as Graphics2D
            try {
                g2.color = previewBackgroundColor()
                g2.fillRect(0, 0, width, height)
                val img = image ?: return
                if (img.width <= 0 || img.height <= 0) return
                val scale = minOf(width.toDouble() / img.width, height.toDouble() / img.height)
                val w = (img.width * scale).toInt().coerceAtLeast(1)
                val h = (img.height * scale).toInt().coerceAtLeast(1)
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
                g2.drawImage(img, (width - w) / 2, (height - h) / 2, w, h, null)
            } finally {
                g2.dispose()
            }
        }
    }

    companion object {
        private const val PREVIEW_WIDTH_PX = 300
        private const val PREVIEW_HEIGHT_PX = 100
    }
}
