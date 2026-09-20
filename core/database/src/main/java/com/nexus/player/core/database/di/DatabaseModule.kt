package com.nexus.player.core.database.di

import android.content.Context
import androidx.room.Room
import com.nexus.player.core.database.NexusDatabase
import com.nexus.player.core.database.dao.PlaylistDao
import com.nexus.player.core.database.dao.VideoDao
import com.nexus.player.core.database.repository.FolderRepository
import com.nexus.player.core.database.repository.FolderRepositoryImpl
import com.nexus.player.core.database.repository.PlaylistRepository
import com.nexus.player.core.database.repository.PlaylistRepositoryImpl
import com.nexus.player.core.database.repository.VideoRepository
import com.nexus.player.core.database.repository.VideoRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DatabaseModule {

    @Binds
    @Singleton
    abstract fun bindVideoRepository(
        impl: VideoRepositoryImpl
    ): VideoRepository

    @Binds
    @Singleton
    abstract fun bindFolderRepository(
        impl: FolderRepositoryImpl
    ): FolderRepository

    @Binds
    @Singleton
    abstract fun bindPlaylistRepository(
        impl: PlaylistRepositoryImpl
    ): PlaylistRepository

    companion object {

        @Provides
        @Singleton
        fun provideNexusDatabase(
            @ApplicationContext context: Context
        ): NexusDatabase {
            return Room.databaseBuilder(
                context,
                NexusDatabase::class.java,
                NexusDatabase.DATABASE_NAME
            )
                .addMigrations(
                    NexusDatabase.MIGRATION_1_2,
                    NexusDatabase.MIGRATION_2_3
                )
                .build()
        }

        @Provides
        fun provideVideoDao(database: NexusDatabase): VideoDao {
            return database.videoDao()
        }

        @Provides
        fun providePlaylistDao(database: NexusDatabase): PlaylistDao {
            return database.playlistDao()
        }
    }
}
