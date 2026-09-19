package com.nexus.player

import org.junit.Assert.assertNotNull
import org.junit.Test

class AppSanityTest {

    @Test
    fun applicationClass_isInstantiable() {
        val app = NexusApplication()
        assertNotNull(app)
    }
}
