package com.danneu.kog

import org.junit.Assert.*
import org.junit.Test

// TODO: Finish these tests

class EnvTests {
    @Test
    fun testNullOverride() {
        val env1 = EnvContainer(mapOf("KEY" to "42"))
        val env2 = env1.fork(mapOf("KEY" to null))
        assertEquals(42, env1.int("KEY"))
        assertEquals(null, env2.int("KEY"))
        assertEquals("null overrides get removed from map", 0, env2.env.size)
    }

    @Test
    fun testBool() {
        assertEquals("\"true\" is true", true, EnvContainer(mapOf("KEY" to "true")).bool("KEY"))
        assertEquals("\"1\" is true", true, EnvContainer(mapOf("KEY" to "1")).bool("KEY"))
        // Nothing else parses to true
        assertEquals(false, EnvContainer(mapOf("KEY" to "test")).bool("KEY"))
        assertEquals(false, EnvContainer(mapOf("KEY" to "42")).bool("KEY"))
        assertEquals(false, EnvContainer(emptyMap()).bool("KEY"))
    }
}

