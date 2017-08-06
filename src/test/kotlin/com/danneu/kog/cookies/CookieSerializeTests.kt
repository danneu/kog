package com.danneu.kog.cookies

import org.junit.Assert.*
import org.junit.Test
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneOffset

class CookieSerializeTests {
    @Test
    fun testBasic1() {
        assertEquals("foo=bar", Cookie("bar").serialize("foo"))
    }

    @Test
    fun testBasic2() {
        assertEquals("foo=bar%20baz", Cookie("bar baz").serialize("foo"))
    }

    @Test
    fun testBasic3() {
        assertEquals("foo=", Cookie("").serialize("foo"))
    }

    @Test(expected = SerializeException::class)
    fun testBadKey1() {
        Cookie("bar").serialize("foo\n")
    }

    @Test(expected = SerializeException::class)
    fun testBadKey2() {
        Cookie("bar").serialize("foo\u280a")
    }

    @Test
    fun testEscaping() {
        assertEquals("foo=%2B%20", Cookie("+ ").serialize("foo"))
    }

    @Test
    fun testParseThenSerialize1() {
        assertEquals(
            mutableMapOf("cat" to "foo=123&name=baz five"),
            parse(Cookie("foo=123&name=baz five").serialize("cat"))
        )
    }

    @Test
    fun testParseThenSerialize2() {
        assertEquals(
            mutableMapOf("cat" to " \";/"),
            parse(Cookie(" \";/").serialize("cat"))
        )
    }

    @Test
    fun testKitchenSink() {
        assertEquals(
            "foo=bar; Max-Age=0; Domain=example.com; Path=/foo; HttpOnly; Secure; First-Party-Only; SameSite=Strict",
            Cookie(
                "bar",
                duration = Ttl.MaxAge(Duration.ZERO),
                domain = "example.com",
                path = "/foo",
                httpOnly = true,
                secure = true,
                firstPartyOnly = true,
                sameSite = SameSite.Strict
            ).serialize("foo")
        )
    }

    //
    // COOKIE OPTIONS
    //

    // Option: path

    @Test
    fun testPath() {
        assertEquals("foo=bar; Path=/", Cookie("bar", path = "/").serialize("foo"))
    }

    @Test(expected = SerializeException::class)
    fun testBadPath() {
        Cookie("bar", path = "\n").serialize("foo")
    }

    // Option: Secure

    @Test
    fun testSecureDefault() {
        assertTrue("Secure is false by default", !Cookie("bar").secure)
    }

    @Test
    fun testSecureTrue() {
        assertEquals("foo=bar; Secure", Cookie("bar", secure = true).serialize("foo"))
    }

    @Test
    fun testSecureFalse() {
        assertEquals("foo=bar", Cookie("bar", secure = false).serialize("foo"))
    }

    // Option: Domain

    @Test
    fun testDomain() {
        assertEquals("foo=bar; Domain=example.com", Cookie("bar", domain = "example.com").serialize("foo"))
    }

    @Test(expected = SerializeException::class)
    fun testBadDomain() {
        Cookie("bar", domain = "example.com\n").serialize("foo")
    }

    // Option: HttpOnly

    @Test
    fun testHttpOnlyDefault() {
        assertTrue("HttpOnly is false by default", !Cookie("bar").httpOnly)
    }

    @Test
    fun testHttpOnlyTrue() {
        assertEquals("foo=bar; HttpOnly", Cookie("bar", httpOnly = true).serialize("foo"))
    }

    @Test
    fun testHttpOnlyFalse() {
        assertEquals("foo=bar", Cookie("bar", httpOnly = false).serialize("foo"))
    }

    // Option: Duration (Max-Age, Expires, Session)

    @Test
    fun testSession() {
        assertEquals("foo=bar", Cookie("bar", duration = Ttl.Session).serialize("foo"))
    }

    @Test
    fun testMaxAge() {
        assertEquals("foo=bar; Max-Age=1000", Cookie("bar", duration = Ttl.MaxAge(Duration.ofSeconds(1000))).serialize("foo"))
        assertEquals("foo=bar; Max-Age=0", Cookie("bar", duration = Ttl.MaxAge(Duration.ZERO)).serialize("foo"))
    }

    @Test
    fun testExpires() {
        assertEquals(
            "foo=bar; Expires=Sun, 24 Dec 2000 10:30:59 GMT",
            Cookie("bar", duration = Ttl.Expires(OffsetDateTime.of(2000, 12, 24, 10, 30, 59, 0, ZoneOffset.UTC))).serialize("foo")
        )
    }

    // Option: SameSite

    @Test
    fun testSameSiteStrict() {
        assertEquals("foo=bar; SameSite=Strict", Cookie("bar", sameSite = SameSite.Strict).serialize("foo"))
    }

    @Test
    fun testSameSiteLax() {
        assertEquals("foo=bar; SameSite=Lax", Cookie("bar", sameSite = SameSite.Lax).serialize("foo"))
    }
}
