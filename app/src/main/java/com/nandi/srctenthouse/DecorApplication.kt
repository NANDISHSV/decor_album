package com.nandi.srctenthouse

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache

class DecorApplication : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25) // 25% of app memory for decoded bitmaps
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.03) // ~3% of device storage for cached originals
                    .build()
            }
            .crossfade(false) // swipe -> image, no fade delay
            .respectCacheHeaders(false) // Firebase Storage URLs are stable; always trust our cache
            .build()
    }
}