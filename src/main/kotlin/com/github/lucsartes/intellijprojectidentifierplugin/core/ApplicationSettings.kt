package com.github.lucsartes.intellijprojectidentifierplugin.core

/**
 * Application-level (global) plugin settings. Pure domain model with no IntelliJ dependencies.
 * These settings apply to all projects and are stored in IDE application-level configuration.
 */
data class ApplicationSettings(
    /**
     * Set of words/tokens to ignore when generating project identifiers.
     * Matching is case-insensitive.
     *
     * Example: If ["TR", "TRE"] are ignored, "tr-my-project" generates "MP" instead of "TMP".
     */
    val ignoredWords: Set<String> = emptySet()
)
