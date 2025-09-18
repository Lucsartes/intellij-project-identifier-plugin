package com.github.lucsartes.intellijprojectidentifierplugin.core

import com.github.lucsartes.intellijprojectidentifierplugin.ports.IdentifierService
import java.util.logging.Logger

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

    private val log: Logger = Logger.getLogger(IdentifierServiceImpl::class.java.name)

    override fun generate(projectName: String): String {
        if (projectName.isBlank()) {
            log.info("Identifier generate called with blank projectName; returning empty identifier")
            return ""
        }

        log.info("Generating identifier for projectName='${projectName.take(CoreDefaults.LOG_PREVIEW_LENGTH)}' (len=${projectName.length})")

        // Find sequences of letters or digits (Unicode-aware), take their first char
        val builder = StringBuilder()
        var tokenCount = 0
        for (match in CoreDefaults.TOKEN_REGEX.findAll(projectName)) {
            val firstChar = match.value.first()
            builder.append(firstChar.uppercaseChar())
            tokenCount++
        }
        val result = builder.toString()
        log.info("Identifier generated: '$result' (tokens=$tokenCount)")
        return result
    }
}
