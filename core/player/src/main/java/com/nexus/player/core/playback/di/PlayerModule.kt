package com.nexus.player.core.playback.di

import com.nexus.player.core.playback.NexusPlayer
import com.nexus.player.core.playback.internal.Media3PlayerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing the [NexusPlayer] binding.
 *
 * Scoped as [Singleton] to ensure a single player instance across the app,
 * matching the typical lifecycle of a video player (one active playback at a time).
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class PlayerModule {

    @Binds
    @Singleton
    abstract fun bindNexusPlayer(
        impl: Media3PlayerImpl
    ): NexusPlayer

    @Binds
    @Singleton
    abstract fun bindSubtitleRepository(
        impl: com.nexus.player.core.playback.repository.SubtitleRepositoryImpl
    ): com.nexus.player.core.playback.repository.SubtitleRepository
}
