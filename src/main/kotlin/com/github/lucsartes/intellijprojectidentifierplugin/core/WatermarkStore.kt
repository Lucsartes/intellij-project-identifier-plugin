package com.github.lucsartes.intellijprojectidentifierplugin.core

import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

/**
 * Pure filesystem helper that persists the generated watermark PNG for a project.
 *
 * Each project's files live in their own subdirectory (keyed by a stable per-project token), so
 * cleanup for one project can never delete another project's watermark — even when project names
 * share a prefix (e.g. "shop" vs "shop-api"). Filenames are length-bounded to stay within the
 * filesystem's per-component limit; uniqueness comes from a random token, so bounding never collides.
 */
class WatermarkStore(private val baseDir: Path) {

    /**
     * Removes any previous watermark files for THIS project only, writes [bytes] for it, and returns
     * the path written.
     *
     * @param projectKey a stable, project-unique key (e.g. the IDE project location hash).
     * @param projectName used only to make the filename human-recognizable.
     */
    fun write(projectKey: String, projectName: String, text: String, bytes: ByteArray): Path {
        val projectDir = baseDir.resolve(directoryName(projectKey))
        Files.createDirectories(projectDir)
        cleanup(projectDir)
        val fileName = "${cap(sanitize(projectName), MAX_NAME_LEN)}-${cap(sanitize(text), MAX_TEXT_LEN)}-${token()}.png"
        val target = projectDir.resolve(fileName)
        Files.write(target, bytes)
        return target
    }

    /** Best-effort removal of prior PNGs in this project's own directory. */
    private fun cleanup(projectDir: Path) {
        runCatching {
            Files.list(projectDir).use { stream ->
                stream.filter { it.fileName.toString().endsWith(".png") }
                    .forEach { runCatching { Files.deleteIfExists(it) } }
            }
        }
    }

    /** Bounds the subdirectory name; falls back to a hash suffix so a pathologically long key stays valid and unique. */
    private fun directoryName(projectKey: String): String {
        val safe = sanitize(projectKey)
        if (safe.length <= MAX_DIR_LEN) return safe
        val suffix = Integer.toHexString(projectKey.hashCode())
        return safe.take(MAX_DIR_LEN - suffix.length - 1) + "-" + suffix
    }

    private fun token(): String = UUID.randomUUID().toString().replace("-", "").take(10)

    private fun cap(value: String, max: Int): String = if (value.length <= max) value else value.take(max)

    companion object {
        private const val MAX_NAME_LEN = 60
        private const val MAX_TEXT_LEN = 80
        private const val MAX_DIR_LEN = 120
        private val UNSAFE_CHARS = Regex("[^A-Za-z0-9._-]")
        private fun sanitize(value: String): String = value.replace(UNSAFE_CHARS, "_")
    }
}
