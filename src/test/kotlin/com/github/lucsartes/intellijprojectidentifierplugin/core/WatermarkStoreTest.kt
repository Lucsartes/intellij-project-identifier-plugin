package com.github.lucsartes.intellijprojectidentifierplugin.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path

class WatermarkStoreTest {

    private fun tempStore(): Pair<WatermarkStore, Path> {
        val base = Files.createTempDirectory("watermark-store-test")
        return WatermarkStore(base) to base
    }

    private fun pngCount(dir: Path): Long =
        Files.list(dir).use { stream -> stream.filter { it.fileName.toString().endsWith(".png") }.count() }

    @Test
    fun write_storesPngInPerProjectSubdirectory() {
        val (store, base) = tempStore()
        val path = store.write("proj-key", "My Project", "MP", byteArrayOf(1, 2, 3))
        assertTrue(Files.exists(path))
        assertTrue(path.fileName.toString().endsWith(".png"))
        assertEquals(base.resolve("proj-key"), path.parent)
    }

    @Test
    fun write_doesNotDeleteAnotherProjectWhoseNameIsAPrefix() {
        // Reproduces the cross-project cleanup collision: "shop" must not wipe "shop-api".
        val (store, _) = tempStore()
        val shopApiFile = store.write("shop-api", "shop-api", "SA", byteArrayOf(1))
        store.write("shop", "shop", "S1", byteArrayOf(2))
        store.write("shop", "shop", "S2", byteArrayOf(3)) // rerun for "shop" cleans only its own directory
        assertTrue("shop-api watermark must survive shop's cleanup", Files.exists(shopApiFile))
    }

    @Test
    fun write_rewriteLeavesExactlyOneFileForTheProject() {
        val (store, _) = tempStore()
        store.write("k", "proj", "A", byteArrayOf(1))
        val second = store.write("k", "proj", "B", byteArrayOf(2))
        assertEquals(1L, pngCount(second.parent))
        assertTrue(Files.exists(second))
    }

    @Test
    fun write_capsLongTextWithinFilesystemComponentLimit() {
        val (store, _) = tempStore()
        val longText = "feature/".repeat(60) // ~480 chars, e.g. a very long branch name
        val path = store.write("k", "proj", longText, byteArrayOf(1))
        assertTrue(Files.exists(path))
        assertTrue(
            "filename component must stay within the 255-byte limit",
            path.fileName.toString().toByteArray().size <= 255,
        )
    }
}
