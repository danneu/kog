package com.danneu.kog

import org.junit.Assert.*
import org.junit.Test

class StatusTests {
    @Test
    fun testSpaceCamel() {
        assertEquals("Im A Teapot", Status.ImATeapot.toString())
        assertEquals("Unknown", Status.Unknown.toString())
    }
}

class ResponseTests {
    @Test
    fun testStatusShorthand() {
        assertEquals(Status.Ok, Response.ok().status)
    }
}

