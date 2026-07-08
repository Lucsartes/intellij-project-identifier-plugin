package com.github.lucsartes.intellijprojectidentifierplugin.ports

import com.github.lucsartes.intellijprojectidentifierplugin.core.ProjectSettings

/**
 * Port for persisting and retrieving per-project plugin settings.
 * Boundary to the IDE's persistence; pure contract, free of IntelliJ SDK types.
 */
interface ProjectSettingsPort {
    /** Load the current settings, or return defaults if none are persisted. */
    fun load(): ProjectSettings

    /** Persist the provided settings. */
    fun save(settings: ProjectSettings)
}
