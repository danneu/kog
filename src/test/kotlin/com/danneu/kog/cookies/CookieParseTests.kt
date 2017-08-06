package com.danneu.kog.cookies

import org.junit.Assert.*
import org.junit.Test

class CookieParseTests {
    @Test
    fun testBasic() {
        assertEquals(mutableMapOf("foo" to "bar"), parse("foo=bar"))
        assertEquals(mutableMapOf("foo" to "123"), parse("foo=123"))
    }
    @Test
    fun testIgnoresSpaces() {
        assertEquals(mutableMapOf("FOO" to "bar", "baz" to "raz"), parse("FOO  = bar;  baz  =  raz"))
    }
    @Test
    fun testEscaping1() {
        assertEquals(
            mutableMapOf("foo" to "bar=123456789&name=Magic Mouse"),
            parse("foo=\"bar=123456789&name=Magic+Mouse\"")
        )
    }
    @Test
    fun testEscaping2() {
        assertEquals(mutableMapOf("email" to " \",;/"), parse("email=%20%22%2c%3b%2f"))
    }
    @Test
    fun testIgnoreEscapingError() {
        assertEquals(mutableMapOf("foo" to "%1", "bar" to "bar"), parse("foo=%1;bar=bar"))
    }
    @Test
    fun testIgnoreNonValues() {
        assertEquals(mutableMapOf("foo" to "%1", "bar" to "bar"), parse("foo=%1;bar=bar;HttpOnly;Secure"))
    }
    @Test
    fun testDates() {
        assertEquals(
            mutableMapOf("priority" to "true", "Path" to "/", "expires" to "Wed, 29 Jan 2014 17:43:25 GMT"),
            parse("priority=true; expires=Wed, 29 Jan 2014 17:43:25 GMT; Path=/")
        )
    }
    @Test
    fun testAssignOnlyOnce1() {
        assertEquals(mutableMapOf("foo" to "%1", "bar" to "bar"), parse("foo=%1;bar=bar;foo=boo"))
    }
    @Test
    fun testAssignOnlyOnce2() {
        assertEquals(mutableMapOf("foo" to "false", "bar" to "bar"), parse("foo=false;bar=bar;foo=true"))
    }
    @Test
    fun testAssignOnlyOnce3() {
        assertEquals(mutableMapOf("foo" to "", "bar" to "bar"), parse("foo=;bar=bar;foo=boo"))
    }
}
