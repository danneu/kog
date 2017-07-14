package com.danneu.kog

import org.junit.Assert.*
import org.junit.Test


class MimeTests {
    @Test
    fun testEquality() {
        assertEquals("two raws are case-insensitive equal", Mime.Raw("foo"), Mime.Raw("FOO"))
        assertEquals("two named are equal", Mime.Html, Mime.Html)
        assertEquals("raw is equal name of same case-insensitive string", Mime.Raw("tExT/HtML"), Mime.Html)
    }
}
