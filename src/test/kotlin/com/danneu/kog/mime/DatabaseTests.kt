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
        assertEquals(Mime.Html, database.fromExtension("html") )
        assertEquals("extension can be prefixed with dot", Mime.Html, database.fromExtension(".html") )
        assertEquals("extension will be trimmed", Mime.Html, database.fromExtension("  .html  ") )
        assertNull(database.fromExtension("not found"))

        assertTrue(database.fromExtension("html") == Mime.Html)
        assertTrue(database.fromExtension("js") == Mime.Javascript)
        assertTrue(database.fromExtension("x-bogus") == null)
        assertTrue(database.fromExtension("rtf") == Mime.Raw("text/rtf"))
        assertTrue(database.fromExtension("rtf") == Mime.RichTextFormat)
        assertTrue(database.fromExtension("txt") == Mime.Text)
        assertTrue(database.fromExtension("xml") == Mime.Xml)
        assertTrue(database.fromExtension("  .xml") == Mime.Xml)
        assertTrue(database.fromExtension(".bogus") == null)
    }

    @Test
    fun testDefaultCharset() {
        assertEquals(
            "json is utf-8 by default according to mime-db",
            Charsets.UTF_8,
            database.getCharset(Mime.Json)
        )
        assertEquals(
            "html is utf-8 by default because it's text/*",
            Charsets.UTF_8,
            database.getCharset(Mime.Html)
        )
        assertEquals(
            "raw(html) should be utf-8 because text/*",
            Charsets.UTF_8,
            database.getCharset(Mime.Raw("text/html"))
        )
        assertEquals(
            "even bogus mime should be utf-8 if text/*",
            Charsets.UTF_8,
            database.getCharset(Mime.Raw("text/x-bogus"))
        )
        assertNotEquals(
            "application/x-bogus will not be utf-8 since it is not listed in mime-db as utf-8 ",
            Charsets.UTF_8,
            database.getCharset(Mime.Raw("application/x-bogus"))
        )
        assertNotEquals(
            "octet-stream is not utf-8",
            Charsets.UTF_8,
            database.getCharset(Mime.OctetStream)
        )
    }

    @Test
    fun getExtensions() {
        assertEquals(
            "bin",
            database.getExtensions(Mime.OctetStream).firstOrNull()
        )
        assertTrue(
            database.getExtensions(Mime.Html).contains("html")
        )
        assertTrue(
            database.getExtensions(Mime.Raw("application/x-bogus")).isEmpty()
        )
    }
}


