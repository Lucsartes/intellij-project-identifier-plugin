package com.github.lucsartes.intellijprojectidentifierplugin.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TemplateResolverTest {

    private val resolver = TemplateResolver()

    @Test
    fun resolve_substitutesBranch() {
        assertEquals("XXX - main", resolver.resolve("XXX - \${branch}", mapOf("branch" to "main")))
    }

    @Test
    fun resolve_substitutesAllOccurrences() {
        assertEquals("dev/dev", resolver.resolve("\${branch}/\${branch}", mapOf("branch" to "dev")))
    }

    @Test
    fun resolve_nullBranchBecomesEmptyWithoutTrimming() {
        assertEquals("XXX -  - YYY", resolver.resolve("XXX - \${branch} - YYY", mapOf("branch" to null)))
    }

    @Test
    fun resolve_templateWithoutPlaceholderIsUnchanged() {
        assertEquals("XXX", resolver.resolve("XXX", mapOf("branch" to "main")))
    }

    @Test
    fun resolve_preservesSlashesAndUnicodeVerbatim() {
        assertEquals("feature/JIRA-123", resolver.resolve("\${branch}", mapOf("branch" to "feature/JIRA-123")))
        assertEquals("café-日本", resolver.resolve("\${branch}", mapOf("branch" to "café-日本")))
    }

    @Test
    fun resolve_leavesUnknownPlaceholderLiteral() {
        assertEquals("\${foo}", resolver.resolve("\${foo}", mapOf("branch" to "main")))
    }

    @Test
    fun usesPlaceholder_isCaseSensitiveAndDetectsBranch() {
        assertTrue(resolver.usesPlaceholder("[\${branch}] wip", TemplateResolver.BRANCH))
        assertFalse(resolver.usesPlaceholder("\${BRANCH}", TemplateResolver.BRANCH))
        assertFalse(resolver.usesPlaceholder("", TemplateResolver.BRANCH))
        assertFalse(resolver.usesPlaceholder("plain text", TemplateResolver.BRANCH))
    }
}
