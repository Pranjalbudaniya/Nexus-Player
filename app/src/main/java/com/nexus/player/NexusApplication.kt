package com.nexus.player

import android.app.Application
import android.content.ComponentCallbacks2
import com.nexus.player.core.media.thumbnail.ThumbnailLoader
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class NexusApplication : Application() {

    @Inject
    lateinit var thumbnailLoader: ThumbnailLoader

    @Suppress("DEPRECATION")
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= ComponentCallbacks2.TRIM_MEMORY_MODERATE) {
            thumbnailLoader.clearMemoryCache()
        }
    }
}
