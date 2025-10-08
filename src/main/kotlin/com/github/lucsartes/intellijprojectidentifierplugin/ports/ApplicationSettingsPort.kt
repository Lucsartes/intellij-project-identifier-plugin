package com.github.lucsartes.intellijprojectidentifierplugin.ports

import com.github.lucsartes.intellijprojectidentifierplugin.core.ApplicationSettings

/**
 * Port for accessing and persisting application-level (global) settings.
 * Pure domain contract (no IntelliJ dependencies).
 *
 * Application-level settings apply to all projects and are stored in IDE application configuration.
 */
interface ApplicationSettingsPort {
    /**
     * Load the current application-level settings.
     */
    fun load(): ApplicationSettings

    /**
     * Save the given application-level settings and persist them.
     */
    fun save(settings: ApplicationSettings)
}
