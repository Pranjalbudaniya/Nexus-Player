package com.nexus.player.core.common.network

import javax.inject.Qualifier
import kotlin.annotation.AnnotationRetention.RUNTIME

@Qualifier
@Retention(RUNTIME)
annotation class Dispatcher(val dispatcher: NexusDispatchers)

enum class NexusDispatchers {
    Default,
    IO,
    Main
}
