package com.github.lucsartes.intellijprojectidentifierplugin.adapters.intellij

import com.github.lucsartes.intellijprojectidentifierplugin.core.ProjectSettings
import com.github.lucsartes.intellijprojectidentifierplugin.ports.ProjectSettingsPort
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.util.messages.Topic

/**
 * IntelliJ project-level service that persists ProjectSettings and fulfills the ProjectSettingsPort.
 * Lives in the adapters layer to isolate IntelliJ SDK types.
 */
@State(
    name = "ProjectIdentifierSettings",
    storages = [Storage(StoragePathMacros.WORKSPACE_FILE)]
)
class IntelliJSettingsService(private val project: Project) : PersistentStateComponent<IntelliJSettingsService.State>, ProjectSettingsPort {

    interface SettingsChangedListener {
        fun settingsChanged(newSettings: ProjectSettings)
    }

    companion object {
        val TOPIC: Topic<SettingsChangedListener> = Topic.create(
            "ProjectIdentifierSettingsChanged",
            SettingsChangedListener::class.java
        )
    }

    data class State(
        var identifierOverride: String? = null,
        var fontFamily: String? = null,
        var fontSizePx: Int? = null,
        var textColorArgb: Int? = null,
    ) {
        fun toDomain(): ProjectSettings = ProjectSettings(
            identifierOverride = identifierOverride?.ifBlank { null },
            fontFamily = fontFamily?.ifBlank { null },
            fontSizePx = fontSizePx,
            textColorArgb = textColorArgb
        )

        companion object {
            fun fromDomain(settings: ProjectSettings): State = State(
                identifierOverride = settings.identifierOverride?.ifBlank { null },
                fontFamily = settings.fontFamily?.ifBlank { null },
                fontSizePx = settings.fontSizePx,
                textColorArgb = settings.textColorArgb
            )
        }
    }

    private val log = Logger.getInstance(IntelliJSettingsService::class.java)

    private var state: State = State()

    // PersistentStateComponent implementation
    override fun getState(): State {
        log.info("getState called: override=${state.identifierOverride}, fontFamily=${state.fontFamily}, fontSizePx=${state.fontSizePx}, textColorArgb=${state.textColorArgb}")
        return state
    }

    override fun loadState(state: State) {
        log.info("Loading persisted settings state: override=${state.identifierOverride}, fontFamily=${state.fontFamily}, fontSizePx=${state.fontSizePx}, textColorArgb=${state.textColorArgb}")
        this.state = state
    }

    // ProjectSettingsPort implementation
    override fun load(): ProjectSettings {
        val s = state.toDomain()
        log.info("Settings loaded (toDomain): override=${s.identifierOverride}, fontFamily=${s.fontFamily}, fontSizePx=${s.fontSizePx}, textColorArgb=${s.textColorArgb}")
        return s
    }

    override fun save(settings: ProjectSettings) {
        log.info("Saving settings: override=${settings.identifierOverride}, fontFamily=${settings.fontFamily}, fontSizePx=${settings.fontSizePx}, textColorArgb=${settings.textColorArgb}")
        this.state = State.fromDomain(settings)
        // publish settings changed event
        runCatching {
            project.messageBus
                .syncPublisher(TOPIC)
                .settingsChanged(load())
        }.onSuccess {
            log.info("Settings change event published")
        }.onFailure { t ->
            log.warn("Failed to publish settings change event", t)
        }
    }
}
