package com.github.lucsartes.intellijprojectidentifierplugin.core

/**
 * Derives an acronym-like identifier from a project name by taking the first character of each
 * alphanumeric token and uppercasing it. Pure domain logic with no IntelliJ dependencies.
 *
 * Examples:
 *  - "My Awesome Project" -> "MAP"
 *  - "  many   spaces  here " -> "MSH"
 *  - "foo-bar_baz" -> "FBB"
 */
class IdentifierGenerator {

    fun generate(projectName: String, ignoredWords: Set<String> = emptySet()): String {
        if (projectName.isBlank()) return ""

        // Normalize ignored words to lowercase for case-insensitive comparison
        val normalizedIgnoredWords = ignoredWords.map { it.lowercase() }.toSet()

        // Find sequences of letters or digits (Unicode-aware) and take the first char of each
        val builder = StringBuilder()
        for (match in CoreDefaults.TOKEN_REGEX.findAll(projectName)) {
            val token = match.value
            if (normalizedIgnoredWords.contains(token.lowercase())) continue
            builder.append(token.first().uppercaseChar())
        }
        return builder.toString()
    }
}
