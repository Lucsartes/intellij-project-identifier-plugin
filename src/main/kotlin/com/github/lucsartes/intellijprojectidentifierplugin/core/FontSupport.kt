package com.github.lucsartes.intellijprojectidentifierplugin.core

/**
 * Pure, IDE-agnostic policy for choosing which font family to render the watermark with.
 *
 * This is the single source of truth for the preferred default and the generic fallback, so the
 * settings UI, the pipeline and the renderer all agree instead of each hard-coding font names.
 */
object FontSupport {

    /** Family preferred when the user has not chosen one, used only if the JRE reports it available. */
    const val PREFERRED_DEFAULT: String = "JetBrains Mono"

    /** Generic family always available on the JVM, used when nothing better can be honored. */
    const val FALLBACK: String = "SansSerif"

    /**
     * Resolves the family to render with.
     *
     * @param requested the user-selected family, or null/blank for "no explicit choice".
     * @param available the families the JRE reports; an empty set means "unknown" (do not filter).
     *
     * - An explicit choice is honored when available (or when availability is unknown), else [FALLBACK].
     * - With no explicit choice, [PREFERRED_DEFAULT] is used when available, else [FALLBACK].
     */
    fun resolveFamily(requested: String?, available: Set<String>): String {
        val explicit = requested?.takeIf { it.isNotBlank() }
        if (explicit != null) {
            return if (available.isEmpty() || explicit in available) explicit else FALLBACK
        }
        return if (PREFERRED_DEFAULT in available) PREFERRED_DEFAULT else FALLBACK
    }
}
