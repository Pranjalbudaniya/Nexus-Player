package com.nexus.player.core.common

import com.nexus.player.core.common.result.Result
import com.nexus.player.core.common.result.asResult
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ResultTest {

    @Test
    fun flow_emitsLoadingThenSuccess() = runBlocking {
        val flow = flow { emit("Nexus") }.asResult()
        val results = flow.toList()

        assertEquals(2, results.size)
        assertTrue(results[0] is Result.Loading)
        assertEquals(Result.Success("Nexus"), results[1])
    }

    @Test
    fun flow_emitsLoadingThenError() = runBlocking {
        val flow = flow<String> { throw IllegalStateException("Test error") }.asResult()
        val results = flow.toList()

        assertEquals(2, results.size)
        assertTrue(results[0] is Result.Loading)
        assertTrue(results[1] is Result.Error)
        assertEquals("Test error", (results[1] as Result.Error).message)
    }
}
