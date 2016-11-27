package com.danneu.kog

import org.junit.Assert.*
import org.junit.Test


// Some HEAD-related tests are in RouterTests


// TODO: Make some sort of ToyServer so that I can assert against my server middleware stack at once
// which would make for better tests than asserting against isolated/a-la-carte serverware.
// for instance, it would also test server middleware ordering.


class HeadTests {
    @Test
    fun testBody() {
        val handler: Handler = Server.wrapHead()({ Response().text("abc") })
        val response = handler(Request.toy(method = Method.head))
        assertEquals("body is removed", ResponseBody.None, response.body)
    }


    @Test
    fun testHeadersPreserved() {
        val handler: Handler = Server.wrapHead()({
            Response().text("abc")
                .setHeader("content-length", "3")
                .setHeader("foo", "bar")
        })
        val response = handler(Request.toy(method = Method.head))
        assertEquals("header 'content-length' still set", "3", response.getHeader("content-length"))
        assertEquals("custom header 'foo' still set", "bar", response.getHeader("foo"))
    }


    @Test
    fun testMethod() {
        val middleware = Server.wrapHead()
        val handler: Handler = { req ->
            assertEquals("method is preserved", Method.head, req.method)
            Response()
        }
        middleware(handler)(Request.toy(method = Method.head))
    }
}

