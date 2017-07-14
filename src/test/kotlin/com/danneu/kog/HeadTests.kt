package com.danneu.kog

import com.danneu.kog.toy
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
        val response = handler(Request.toy(method = Method.Head))
        assertEquals("body is removed", ResponseBody.None, response.body)
    }


    @Test
    fun testHeadersPreserved() {
        val handler: Handler = Server.wrapHead()({
            Response().text("abc")
                .setHeader(Header.ContentLength, "3")
                .setHeader(Header.Custom("foo"), "bar")
        })
        val response = handler(Request.toy(method = Method.Head))
        assertEquals("header 'content-length' still set", "3", response.getHeader(Header.ContentLength))
        assertEquals("custom header 'foo' still set", "bar", response.getHeader(Header.Custom("foo")))
    }


    @Test
    fun testMethod() {
        val middleware = Server.wrapHead()
        val handler: Handler = { req ->
            assertEquals("method is preserved", Method.Head, req.method)
            Response()
        }
        middleware(handler)(Request.toy(method = Method.Head))
    }
}

