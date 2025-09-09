package com.github.lucsartes.intellijprojectidentifierplugin.ports

/**
 * Port for generating a short human-friendly identifier from a project name.
 * Pure domain contract (no IntelliJ dependencies).
 */
interface IdentifierService {
    /**
     * Derive a concise identifier string from the given project name.
     */
    fun generate(projectName: String): String
}