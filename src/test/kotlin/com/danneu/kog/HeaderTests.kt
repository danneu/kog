package com.danneu.kog

import org.junit.Assert.*
import org.junit.Test


// TODO: HasHeaders is a pretty weird thing. Shouldn't need to test it through Request/Response.
// Should be more stand-alone.


class CookieCaseTests {
    @Test
    fun testSingleChar() {
        assertEquals("capitalizes single char", "X", "x".toCookieCase())
    }

    @Test
    fun testWhenDowncased() {
        assertEquals("handles all lower caps", "Foo-Bar", "foo-bar".toCookieCase())
    }

    @Test
    fun testWhenUpcased() {
        assertEquals("handles all upper caps", "Foo-Bar", "FOO-BAR".toCookieCase())
    }

    @Test
    fun testWhenOppositeCased() {
        assertEquals("fixes all wrong cases", "Foo-Bar", "fOO-bAR".toCookieCase())
    }
}


class HeaderStoreTests {
    @Test
    fun testGetCustom() {
        val r = Response().setHeader(Header.Custom("foo-bar"), "42")
        assertEquals("fetches with exact case", "42", r.getHeader(Header.Custom("foo-bar")))
    }
}


class HeaderSerializeTests {
    @Test
    fun testSerializeObject() {
        assertEquals("Set-Cookie", Header.SetCookie.toString())
    }

    @Test
    fun testSerializeCustom() {
        assertEquals("Foo-Bar", Header.Custom("foo-bar").toString())
    }
}


class HeaderEqualityTests {
    // CUSTOM

    @Test
    fun testEqualityWithSameCaps() {
        val a = Header.Custom("foo-bar")
        val b = Header.Custom("foo-bar")
        assertEquals("custom headers are equal when they have the same key capitalization", a, b)
    }

    @Test
    fun testEqualityWithDifferentKeys() {
        val a = Header.Custom("a")
        val b = Header.Custom("b")
        assertNotEquals("custom headers are not equal when they have different keys", a, b)
    }

    @Test
    fun testEqualityWithDifferentCaps() {
        val a = Header.Custom("FOO-bar")
        val b = Header.Custom("foo-BAR")
        assertEquals("custom headers are equal even with different capitalization", a, b)
        assertEquals("Foo-Bar", a.key)
        assertEquals("Foo-Bar", b.key)
    }

    // OBJECT

    @Test
    fun testSameSingleton() {
        val a = Header.SetCookie
        val b = Header.SetCookie
        assertEquals("object/singleton are stucturally equal", a, b)
    }

    @Test
    fun testDifferentSingleton() {
        val a = Header.SetCookie
        val b = Header.SetCookie2
        assertNotEquals("different object/singleton are not stucturally equal", a, b)
    }
}

