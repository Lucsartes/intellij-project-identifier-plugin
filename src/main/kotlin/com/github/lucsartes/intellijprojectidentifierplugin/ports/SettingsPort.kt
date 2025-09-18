package com.github.lucsartes.intellijprojectidentifierplugin.ports

import com.github.lucsartes.intellijprojectidentifierplugin.core.PluginSettings

/**
 * Port for persisting and retrieving plugin settings.
 * Pure domain contract, free of IntelliJ SDK dependencies.
 */
interface SettingsPort {
    /** Load the current settings, or return defaults if none are persisted. */
    fun load(): PluginSettings

    /** Persist the provided settings. */
    fun save(settings: PluginSettings)
}