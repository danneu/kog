package com.danneu.kog.mime

import com.danneu.kog.Mime
import org.junit.Assert.*
import org.junit.Test

class DatabaseTests {
    @Test
    fun testFromExtension() {
        assertEquals(Mime.fromString("application/x-7z-compressed"), database.fromExtension("7z"))
        assertEquals(Mime.fromString("audio/x-aac"), database.fromExtension("aac"))
        assertEquals(Mime.fromString("application/postscript"), database.fromExtension("ai"))
        assertEquals(Mime.fromString("text/cache-manifest"), database.fromExtension("appcache"))
        assertNull(database.fromExtension("not found"))
    }
}


