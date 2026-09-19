package com.nexus.player.core.scanner.di

import com.nexus.player.core.scanner.MediaScanner
import com.nexus.player.core.scanner.MediaScannerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ScannerModule {

    @Binds
    @Singleton
    abstract fun bindMediaScanner(
        impl: MediaScannerImpl
    ): MediaScanner

    @Binds
    @Singleton
    abstract fun bindMediaScanOrchestrator(
        impl: com.nexus.player.core.scanner.orchestrator.MediaScanOrchestratorImpl
    ): com.nexus.player.core.scanner.orchestrator.MediaScanOrchestrator
}
