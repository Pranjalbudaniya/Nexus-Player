package com.nexus.player.core.database.di

import android.content.Context
import androidx.room.Room
import com.nexus.player.core.database.NexusDatabase
import com.nexus.player.core.database.dao.VideoDao
import com.nexus.player.core.database.repository.FolderRepository
import com.nexus.player.core.database.repository.FolderRepositoryImpl
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
            ).build()
        }

        @Provides
        fun provideVideoDao(database: NexusDatabase): VideoDao {
            return database.videoDao()
        }
    }
}
