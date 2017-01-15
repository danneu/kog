package com.danneu.kog.mime

import org.junit.Assert.*
import org.junit.Test

class DatabaseTests {
    @Test
    fun testFromExtension() {
        assertEquals("application/x-7z-compressed", database.fromExtension("7z"))
        assertEquals("audio/x-aac", database.fromExtension("aac"))
        assertEquals("application/postscript", database.fromExtension("ai"))
        assertEquals("text/cache-manifest", database.fromExtension("appcache"))
        assertNull(database.fromExtension("not found"))
    }
}


