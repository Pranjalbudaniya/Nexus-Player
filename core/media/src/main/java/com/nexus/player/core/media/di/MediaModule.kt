package com.nexus.player.core.media.di

import com.nexus.player.core.media.thumbnail.DefaultThumbnailDecoder
import com.nexus.player.core.media.thumbnail.ThumbnailDecoder
import com.nexus.player.core.media.thumbnail.ThumbnailLoader
import com.nexus.player.core.media.thumbnail.ThumbnailLoaderImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MediaModule {

    @Binds
    @Singleton
    abstract fun bindThumbnailDecoder(
        impl: DefaultThumbnailDecoder
    ): ThumbnailDecoder

    @Binds
    @Singleton
    abstract fun bindThumbnailLoader(
        impl: ThumbnailLoaderImpl
    ): ThumbnailLoader
}
