package com.github.lucsartes.intellijprojectidentifierplugin.adapters.intellij

import com.github.lucsartes.intellijprojectidentifierplugin.core.PluginSettings
import com.github.lucsartes.intellijprojectidentifierplugin.ports.SettingsPort
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.messages.Topic

/**
 * IntelliJ application-level service that persists PluginSettings and fulfills the SettingsPort.
 * Lives in the adapters layer to isolate IntelliJ SDK types.
 */
@State(
    name = "ProjectIdentifierSettings",
    storages = [Storage("ProjectIdentifierSettings.xml")]
)
class IntelliJSettingsService : PersistentStateComponent<IntelliJSettingsService.State>, SettingsPort {

    interface SettingsChangedListener {
        fun settingsChanged(newSettings: PluginSettings)
    }

    companion object {
        val TOPIC: Topic<SettingsChangedListener> = Topic.create(
            "ProjectIdentifierSettingsChanged",
            SettingsChangedListener::class.java
        )
    }

    data class State(
        var enabled: Boolean = PluginSettings().enabled,
        var identifierOverride: String? = PluginSettings().identifierOverride,
    ) {
        fun toDomain(): PluginSettings = PluginSettings(
            enabled = enabled,
            identifierOverride = identifierOverride?.ifBlank { null }
        )

        companion object {
            fun fromDomain(settings: PluginSettings): State = State(
                enabled = settings.enabled,
                identifierOverride = settings.identifierOverride?.ifBlank { null }
            )
        }
    }

    private val log = Logger.getInstance(IntelliJSettingsService::class.java)

    private var state: State = State()

    // PersistentStateComponent implementation
    override fun getState(): State {
        log.info("getState called: enabled=${state.enabled}, override=${state.identifierOverride}")
        return state
    }

    override fun loadState(state: State) {
        log.info("Loading persisted settings state: enabled=${state.enabled}, override=${state.identifierOverride}")
        this.state = state
    }

    // SettingsPort implementation
    override fun load(): PluginSettings {
        val s = state.toDomain()
        log.info("Settings loaded (toDomain): enabled=${s.enabled}, override=${s.identifierOverride}")
        return s
    }

    override fun save(settings: PluginSettings) {
        log.info("Saving settings: enabled=${settings.enabled}, override=${settings.identifierOverride}")
        this.state = State.fromDomain(settings)
        // publish settings changed event
        runCatching {
            ApplicationManager.getApplication()
                .messageBus
                .syncPublisher(TOPIC)
                .settingsChanged(load())
        }.onSuccess {
            log.info("Settings change event published")
        }.onFailure { t ->
            log.warn("Failed to publish settings change event", t)
        }
    }
}
