package com.github.lucsartes.intellijprojectidentifierplugin.adapters.intellij

import com.intellij.openapi.wm.impl.IdeBackgroundUtil
import com.intellij.testFramework.ApplicationRule
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.awt.image.BufferedImage

/**
 * Guards the reflective contract [WallpaperCacheReflection] depends on. If a future IDE build
 * renames `PainterHelper$MyImagePainter`, its `ourImageCache` field, or the `ImageLoadSettings`
 * record shape, this test fails loudly (the live-Apply optimisation would otherwise silently
 * degrade to OK/Cancel-only refresh).
 */
class WallpaperCacheReflectionTest {

    @Rule
    @JvmField
    val appRule = ApplicationRule()

    @Test
    fun primeInsertsImageRetrievableUnderPaintersKey() {
        val absolutePath = "/tmp/project-identifier-test/watermark-${System.nanoTime()}.png"
        val image = BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB)

        assertTrue(
            "Reflection into PainterHelper\$MyImagePainter.ourImageCache failed; IDE internals may have changed on this platform build",
            WallpaperCacheReflection.prime(absolutePath, image),
        )

        // Independently reflect the same cache + key to confirm the put is retrievable via the
        // exact key the painter builds for our property spec (path, no flip).
        val loader = IdeBackgroundUtil::class.java.classLoader
        val key = loader.loadClass("com.intellij.openapi.wm.impl.PainterHelper\$MyImagePainter\$ImageLoadSettings")
            .getDeclaredConstructor(String::class.java, Boolean::class.javaPrimitiveType, Boolean::class.javaPrimitiveType)
            .apply { isAccessible = true }
            .newInstance(absolutePath, false, false)
        val cache = loader.loadClass("com.intellij.openapi.wm.impl.PainterHelper\$MyImagePainter")
            .getDeclaredField("ourImageCache")
            .apply { isAccessible = true }
            .get(null) as Map<*, *>

        assertSame(image, cache[key])
    }
}
