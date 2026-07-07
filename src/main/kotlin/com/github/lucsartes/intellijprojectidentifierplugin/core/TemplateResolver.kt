package com.github.lucsartes.intellijprojectidentifierplugin.core

/**
 * Pure, IDE-agnostic resolver for dynamic placeholders embedded in the identifier text.
 *
 * A placeholder has the form `${name}`. Known placeholders are substituted with the value
 * supplied in [values]; a null value substitutes an empty string. Placeholders whose name is
 * not present in [values] are left verbatim, so unrelated `${...}` sequences are preserved.
 *
 * The design is intentionally map-driven so new placeholders (e.g. `${project}`, `${sha}`) can be
 * added later simply by supplying more entries, without changing this class.
 *
 * Substitution is literal and does NOT trim surrounding whitespace or separators, e.g.
 * `resolve("XXX - ${branch} - YYY", mapOf("branch" to null))` yields `"XXX -  - YYY"`.
 */
class TemplateResolver {

    /**
     * Replaces every known placeholder in [template] with its value from [values]
     * (empty string for a null value). Unknown placeholders are returned unchanged.
     */
    fun resolve(template: String, values: Map<String, String?>): String =
        PLACEHOLDER_REGEX.replace(template) { match ->
            val name = match.groupValues[1]
            if (values.containsKey(name)) values[name].orEmpty() else match.value
        }

    /**
     * Returns true if [template] references the placeholder [name] (case-sensitive).
     * Used to decide whether the (potentially costly) value for a dynamic placeholder must be fetched.
     */
    fun usesPlaceholder(template: String, name: String): Boolean =
        PLACEHOLDER_REGEX.findAll(template).any { it.groupValues[1] == name }

    companion object {
        /** Placeholder name for the current VCS branch. */
        const val BRANCH: String = "branch"

        // ${name} where name is one or more ASCII letters, digits or underscores.
        private val PLACEHOLDER_REGEX: Regex = Regex("\\$\\{([A-Za-z0-9_]+)}")
    }
}
