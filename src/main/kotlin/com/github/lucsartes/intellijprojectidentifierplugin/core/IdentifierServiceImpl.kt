package com.github.lucsartes.intellijprojectidentifierplugin.core

import com.github.lucsartes.intellijprojectidentifierplugin.ports.IdentifierService

/**
 * Pure implementation of the IdentifierService that derives an acronym-like
 * identifier from the given project name by taking the first character of each
 * alphanumeric token and uppercasing it.
 *
 * Examples:
 *  - "My Awesome Project" -> "MAP"
 *  - "  many   spaces  here " -> "MSH"
 *  - "foo-bar_baz" -> "FBB"
 */
class IdentifierServiceImpl : IdentifierService {
    override fun generate(projectName: String): String {
        if (projectName.isBlank()) return ""

        // Find sequences of letters or digits (Unicode-aware), take their first char
        val tokenRegex = Regex("[\\p{L}\\p{N}]+")
        val builder = StringBuilder()
        for (match in tokenRegex.findAll(projectName)) {
            val firstChar = match.value.first()
            builder.append(firstChar.uppercaseChar())
        }
        return builder.toString()
    }
}
