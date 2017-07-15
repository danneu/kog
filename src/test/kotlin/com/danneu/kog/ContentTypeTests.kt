package com.danneu.kog

import org.junit.Assert.*
import org.junit.Test


class ContentTypeTestsTests {
    @Test
    fun testParse() {
        assertEquals(
            "text/* mimes default to utf-8 when constructed",
            Charsets.UTF_8,
            ContentType(Mime.Text).charset
        )
        assertEquals(
            "text/* mimes default to utf-8 when parsed",
            Charsets.UTF_8,
            ContentType.parse("text/plain")?.charset
        )
        assertEquals(
            "parses params",
            ContentType(Mime.Text, null, mutableMapOf("foo" to "bar", "hello" to "world")),
            ContentType.parse("text/plain; foo=bar; hello=world")
        )
        assertEquals(
            "moves charset out of params",
            ContentType(Mime.Text, Charsets.UTF_8, mutableMapOf("foo" to "bar", "hello" to "world")),
            ContentType.parse("text/plain; charset=utf-8; foo=bar; hello=world")
        )

        assertEquals(
            "contents are downcased (canonicalized)",
            ContentType(Mime.Html, Charsets.UTF_8, mutableMapOf("foo" to "bar")),
            ContentType.parse("TEXT/HtMl; fOO=BAr")
        )
        assertEquals(
            "existing charset will not be replaced",
            ContentType(Mime.Html, Charsets.ISO_8859_1),
            ContentType.parse("text/html; charset=iso-8859-1")
        )
        assertEquals(
            "charset can be updated later",
            ContentType(Mime.Html, Charsets.US_ASCII),
            ContentType.parse("text/html; charset=iso-8859-1")?.apply {
                this.charset = Charsets.US_ASCII
            }
        )
    }
}
