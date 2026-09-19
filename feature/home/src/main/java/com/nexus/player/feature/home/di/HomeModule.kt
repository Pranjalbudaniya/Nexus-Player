package com.nexus.player.feature.home.di

import com.nexus.player.feature.home.domain.provider.ContinueWatchingSectionProvider
import com.nexus.player.feature.home.domain.provider.ContinueWatchingSectionProviderImpl
import com.nexus.player.feature.home.domain.provider.FavoritesSectionProvider
import com.nexus.player.feature.home.domain.provider.FavoritesSectionProviderImpl
import com.nexus.player.feature.home.domain.provider.FoldersSectionProvider
import com.nexus.player.feature.home.domain.provider.FoldersSectionProviderImpl
import com.nexus.player.feature.home.domain.provider.RecentlyAddedSectionProvider
import com.nexus.player.feature.home.domain.provider.RecentlyAddedSectionProviderImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class HomeModule {

    @Binds
    @Singleton
    abstract fun bindContinueWatchingSectionProvider(
        impl: ContinueWatchingSectionProviderImpl
    ): ContinueWatchingSectionProvider

    @Binds
    @Singleton
    abstract fun bindRecentlyAddedSectionProvider(
        impl: RecentlyAddedSectionProviderImpl
    ): RecentlyAddedSectionProvider

    @Binds
    @Singleton
    abstract fun bindFavoritesSectionProvider(
        impl: FavoritesSectionProviderImpl
    ): FavoritesSectionProvider

    @Binds
    @Singleton
    abstract fun bindFoldersSectionProvider(
        impl: FoldersSectionProviderImpl
    ): FoldersSectionProvider
}
