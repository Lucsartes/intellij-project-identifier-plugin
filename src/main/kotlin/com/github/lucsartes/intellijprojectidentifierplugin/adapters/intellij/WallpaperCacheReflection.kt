package com.github.lucsartes.intellijprojectidentifierplugin.adapters.intellij

import com.intellij.openapi.wm.impl.IdeBackgroundUtil
import java.awt.Image

/**
 * Reflective, best-effort access to the IDE's internal background-image painter cache
 * (`PainterHelper$MyImagePainter.ourImageCache`).
 *
 * When the editor/frame background property changes to a *new* image path, the platform reloads
 * the image on a pooled thread and then swaps it via a non-modal `invokeLater`. While the modal
 * Settings dialog is open that swap is deferred until the dialog closes — so clicking **Apply**
 * (dialog stays open) would not refresh the watermark. By inserting the freshly rendered image into
 * that cache *before* setting the property, the painter's next paint finds a cache hit and applies
 * the image **synchronously**, so Apply refreshes live.
 *
 * This touches unstable IDE internals, so — per [ADR-0001] — it is isolated here and entirely
 * best-effort: [prime] returns `false` (never throws) if the internals differ on a given IDE build,
 * and callers fall back to the plain property write, which the IDE still applies on OK/Cancel.
 */
internal object WallpaperCacheReflection {

    private const val PAINTER_CLASS = "com.intellij.openapi.wm.impl.PainterHelper\$MyImagePainter"
    private const val SETTINGS_CLASS = "com.intellij.openapi.wm.impl.PainterHelper\$MyImagePainter\$ImageLoadSettings"
    private const val CACHE_FIELD = "ourImageCache"

    /**
     * Inserts [image] into the painter cache under the key the painter derives from [absolutePath]
     * (no flip), matching how `loadImageAsync` parses `path,opacity,style,anchor`. The caller MUST
     * keep a strong reference to [image] because the cache stores values weakly.
     *
     * @return `true` if the entry was inserted, `false` on any reflective failure.
     */
    fun prime(absolutePath: String, image: Image): Boolean = runCatching {
        val loader = IdeBackgroundUtil::class.java.classLoader

        val key = loader.loadClass(SETTINGS_CLASS)
            .getDeclaredConstructor(String::class.java, Boolean::class.javaPrimitiveType, Boolean::class.javaPrimitiveType)
            .apply { isAccessible = true }
            .newInstance(absolutePath, false, false)

        @Suppress("UNCHECKED_CAST")
        val cache = loader.loadClass(PAINTER_CLASS)
            .getDeclaredField(CACHE_FIELD)
            .apply { isAccessible = true }
            .get(null) as MutableMap<Any, Any>

        cache[key] = image
        true
    }.getOrDefault(false)
}
