package com.github.lucsartes.intellijprojectidentifierplugin.adapters.intellij

import com.github.lucsartes.intellijprojectidentifierplugin.core.ApplicationSettings
import com.github.lucsartes.intellijprojectidentifierplugin.ports.ApplicationSettingsPort
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.messages.Topic

/**
 * IntelliJ application-level service that persists ApplicationSettings and fulfills the ApplicationSettingsPort.
 * Lives in the adapters layer to isolate IntelliJ SDK types.
 *
 * Application-level means these settings apply to all projects, not just the current one.
 */
@State(
    name = "ProjectIdentifierApplicationSettings",
    storages = [Storage("projectIdentifier.xml")]
)
class IntelliJApplicationSettingsService : PersistentStateComponent<IntelliJApplicationSettingsService.State>, ApplicationSettingsPort {

    interface SettingsChangedListener {
        fun settingsChanged(newSettings: ApplicationSettings)
    }

    companion object {
        val TOPIC = Topic.create("Project Identifier Application Settings Changed", SettingsChangedListener::class.java)
    }

    data class State(
        var ignoredWords: List<String> = emptyList()
    )

    private val log = Logger.getInstance(IntelliJApplicationSettingsService::class.java)

    private var state: State = State()

    // PersistentStateComponent implementation
    override fun getState(): State {
        log.info("getState called: ignoredWords=${state.ignoredWords}")
        return state
    }

    override fun loadState(state: State) {
        log.info("Loading persisted application settings state: ignoredWords=${state.ignoredWords}")
        this.state = state
    }

    // ApplicationSettingsPort implementation
    override fun load(): ApplicationSettings {
        return ApplicationSettings(
            ignoredWords = state.ignoredWords.toSet()
        )
    }

    override fun save(settings: ApplicationSettings) {
        log.info("Saving application settings: ignoredWords=${settings.ignoredWords}")
        val oldState = state
        state = State(
            ignoredWords = settings.ignoredWords.toList()
        )
        if (oldState != state) {
            log.info("Application settings have changed, publishing update.")
            ApplicationManager.getApplication().messageBus.syncPublisher(TOPIC).settingsChanged(load())
        }
    }
}
