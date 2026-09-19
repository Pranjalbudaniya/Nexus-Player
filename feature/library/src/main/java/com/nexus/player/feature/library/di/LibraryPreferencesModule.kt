package com.nexus.player.feature.library.di

import com.nexus.player.feature.library.preferences.LibraryPreferencesRepository
import com.nexus.player.feature.library.preferences.LibraryPreferencesRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface LibraryPreferencesModule {

    @Binds
    @Singleton
    fun bindLibraryPreferencesRepository(
        impl: LibraryPreferencesRepositoryImpl
    ): LibraryPreferencesRepository
}
