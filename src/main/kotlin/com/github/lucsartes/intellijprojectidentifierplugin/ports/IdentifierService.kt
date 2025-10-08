package com.github.lucsartes.intellijprojectidentifierplugin.ports

/**
 * Port for generating a short human-friendly identifier from a project name.
 * Pure domain contract (no IntelliJ dependencies).
 */
interface IdentifierService {
    /**
     * Derive a concise identifier string from the given project name.
     *
     * @param projectName The name of the project to generate an identifier from.
     * @param ignoredWords A set of words to ignore during identifier generation (case-insensitive).
     *                     If a token matches an ignored word, it will be excluded from the identifier.
     *                     Defaults to an empty set if not provided.
     */
    fun generate(projectName: String, ignoredWords: Set<String> = emptySet()): String
}