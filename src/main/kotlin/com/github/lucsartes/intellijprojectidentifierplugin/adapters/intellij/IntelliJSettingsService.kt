package com.github.lucsartes.intellijprojectidentifierplugin.adapters.intellij

import com.github.lucsartes.intellijprojectidentifierplugin.core.PluginSettings
import com.github.lucsartes.intellijprojectidentifierplugin.ports.SettingsPort
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
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
        var opacity: Float = PluginSettings().opacity,
        var identifierOverride: String? = PluginSettings().identifierOverride,
    ) {
        fun toDomain(): PluginSettings = PluginSettings(
            enabled = enabled,
            opacity = opacity.coerceIn(0.0f, 1.0f),
            identifierOverride = identifierOverride?.ifBlank { null }
        )

        companion object {
            fun fromDomain(settings: PluginSettings): State = State(
                enabled = settings.enabled,
                opacity = settings.opacity.coerceIn(0.0f, 1.0f),
                identifierOverride = settings.identifierOverride?.ifBlank { null }
            )
        }
    }

    private var state: State = State()

    // PersistentStateComponent implementation
    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }

    // SettingsPort implementation
    override fun load(): PluginSettings = state.toDomain()

    override fun save(settings: PluginSettings) {
        this.state = State.fromDomain(settings)
        // publish settings changed event
        ApplicationManager.getApplication()
            .messageBus
            .syncPublisher(TOPIC)
            .settingsChanged(load())
    }
}
