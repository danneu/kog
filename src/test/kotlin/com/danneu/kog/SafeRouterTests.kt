package com.danneu.kog

import com.danneu.kog.Method.Delete
import com.danneu.kog.Method.Get
import com.danneu.kog.Method.Post
import com.danneu.kog.Method.Put
import com.danneu.kog.Status.Ok
import com.danneu.kog.Status.NotFound
import com.danneu.kog.sandbox.SafeRouter
import org.junit.Assert.*
import org.junit.Test

class SafeRouterTests {
    // TODO: Add these tests back. Overhauled SafeRouter.

//    @Test
//    fun testStaticMatches() {
//        assertNotNull(Template("/").extractValues("/"))
//        assertNotNull(Template("/hello").extractValues("/hello"))
//        assertNotNull(Template("a").extractValues("a"))
//    }
//
//    @Test
//    fun testParamMatches() {
//        assertNotNull(Template("/<a>", listOf(String::class.createType())).extractValues("/hello"))
//        assertNotNull(Template("/hello/<b>", listOf(String::class.createType())).extractValues("/hello/bob"))
//        assertNotNull(Template("/a/b/<c>/d", listOf(Int::class.createType())).extractValues("/a/b/3/d"))
//    }
//
//    @Test
//    fun testNonMatches() {
//        assertNull(Template("/a/b").extractValues("/a/b/c"))
//        assertNull(Template("/a/b/c/<d>/e").extractValues("/a/b/c/d"))
//        assertNull(Template("a/").extractValues("a"))
//        assertNull(Template("/a").extractValues("/b"))
//    }

    @Test
    fun testRouting() {
        run {
            val router = SafeRouter { get("/foo", fun(): Handler = { Response() }) }
            assertEquals(Ok, router.handler()(Request.toy(Get, "/foo")).status)
        }
        run {
            val router = SafeRouter { get("/<foo>", fun(foo: String): Handler = { Response() }) }
            assertEquals(Ok, router.handler()(Request.toy(Get, "/foo")).status)
            assertEquals(Ok, router.handler()(Request.toy(Get, "/bar")).status)
            assertEquals(Ok, router.handler()(Request.toy(Get, "/42")).status)
        }
        run {
            val router = SafeRouter { get("/<foo>/<bar>", fun(foo: String, bar: Int): Handler = { Response() }) }
            assertEquals(Ok,       router.handler()(Request.toy(Get, "/a/42")).status)
            assertEquals(NotFound, router.handler()(Request.toy(Get, "/42/a")).status)
            assertEquals(Ok,       router.handler()(Request.toy(Get, "/42/42")).status)
            assertEquals(NotFound, router.handler()(Request.toy(Get, "/a/b")).status)
            assertEquals(NotFound, router.handler()(Request.toy(Get, "/a/b/c")).status)
        }
        run {
            val router = SafeRouter {
                get("/a", fun(): Handler = { Response().text("first") })
                get("/a", fun(): Handler = { Response().text("second") })
                get("/a", fun(): Handler = { Response().text("third") })
            }
            assertEquals("matches first route", ResponseBody.String("first"), router.handler()(Request.toy(Get, "/a")).body)
        }
        run {
            val router = SafeRouter {
                post("/a", fun(): Handler = { Response() })
                put("/a", fun(): Handler = { Response() })
                delete("/a", fun(): Handler = { Response() })
            }
            assertEquals("matches POST", Ok, router.handler()(Request.toy(Post, "/a")).status)
            assertEquals("matches PUT", Ok, router.handler()(Request.toy(Put, "/a")).status)
            assertEquals("matches DELETE", Ok, router.handler()(Request.toy(Delete, "/a")).status)
        }
    }

    @Test
    fun testNotFoundRouting() {
        run {
            val router = SafeRouter { get("/<foo>", fun(): Handler = { Response() }) }
            assertEquals("does not match when recv expects no params", NotFound, router.handler()(Request.toy(Get, "/foo")).status)
        }
        run {
            val handler = SafeRouter { get("/foo", fun(): Handler = { Response() }) }.handler()
            assertEquals(NotFound, handler(Request.toy(Post, "/foo")).status)
            assertEquals(NotFound, handler(Request.toy(Delete, "/foo")).status)
            assertEquals(NotFound, handler(Request.toy(Get, "/foo/bar")).status)
            assertEquals(NotFound, handler(Request.toy(Get, "/bar")).status)
        }
        run {
            val handler = SafeRouter { get("/<id>", fun(id: String): Handler = { Response() }) }.handler()
            assertEquals(NotFound, handler(Request.toy(Post, "/foo")).status)
            assertEquals(NotFound, handler(Request.toy(Delete, "/foo")).status)
            assertEquals(NotFound, handler(Request.toy(Get, "/foo/bar")).status)
        }
        run {
            val handler = SafeRouter { get("/<a>/<b>", fun(a: String, b: String): Handler = { Response() }) }.handler()
            assertEquals(NotFound, handler(Request.toy(Get, "/a/b/c")).status)
            assertEquals(NotFound, handler(Request.toy(Get, "/a")).status)
            assertEquals(NotFound, handler(Request.toy(Get, "/a/")).status)
            assertEquals(NotFound, handler(Request.toy(Delete, "/a/b")).status)
            assertEquals(NotFound, handler(Request.toy(Delete, "/a/b/c")).status)
        }
    }

    @Test
    fun testTrailingSlash() {
        run {
            val handler = SafeRouter { get("/a/b", fun(): Handler = { Response() }) }.handler()
            assertEquals(Ok, handler(Request.toy(Get, "/a/b")).status)
            assertEquals(NotFound, handler(Request.toy(Get, "/a/b/")).status)
            assertEquals(NotFound, handler(Request.toy(Get, "/a/b/c")).status)
            assertEquals(NotFound, handler(Request.toy(Delete, "/a/b")).status)
        }
        run {
            val handler = SafeRouter { get("/<a>/<b>", fun(a: String, b: String): Handler = { Response() }) }.handler()
            assertEquals(Ok, handler(Request.toy(Get, "/a/b")).status)
            assertEquals(NotFound, handler(Request.toy(Get, "/a/b/")).status)
        }
    }

    @Test
    fun testParams() {
        run {
            // String
            val handler = SafeRouter { get("/<a>", fun(a: String): Handler = { Response().text(a) }) }.handler()
            assertEquals(ResponseBody.String("a"), handler(Request.toy(Get, "/a")).body)
            assertEquals(ResponseBody.String("foo"), handler(Request.toy(Get, "/foo")).body)
        }
        run {
            // Int
            val handler = SafeRouter { get("/<a>", fun(a: Int): Handler = { Response().text(a.toString()) }) }.handler()
            assertEquals(ResponseBody.String("42"), handler(Request.toy(Get, "/42")).body)
            assertEquals(ResponseBody.String("0"), handler(Request.toy(Get, "/000")).body)
            assertEquals("negative ints not supported", NotFound, handler(Request.toy(Get, "/-1")).status)
        }
        run {
            // Int, Int
            val handler = SafeRouter {
                get("/<a>/<b>", fun(a: Int, b: Int): Handler = { Response().text((a + b).toString()) })
            }.handler()
            assertEquals(ResponseBody.String("3"), handler(Request.toy(Get, "/1/2")).body)
            assertEquals(ResponseBody.String("3"), handler(Request.toy(Get, "/2/1")).body)
            assertEquals(ResponseBody.String("3"), handler(Request.toy(Get, "/3/0")).body)
            assertEquals(ResponseBody.String("3"), handler(Request.toy(Get, "/0/3")).body)
            assertEquals(ResponseBody.String("0"), handler(Request.toy(Get, "/0/0")).body)
            assertEquals(NotFound, handler(Request.toy(Get, "/1/-1")).status)
            assertEquals(NotFound, handler(Request.toy(Get, "/-1/1")).status)
            assertEquals(NotFound, handler(Request.toy(Get, "/a/b")).status)
            assertEquals(NotFound, handler(Request.toy(Get, "/")).status)
        }
        run {
            // Static, Int, Static
            val handler = SafeRouter {
                get("/a/<b>/c", fun(b: Int): Handler = { Response().text(b.toString()) })
            }.handler()
            assertEquals(ResponseBody.String("42"), handler(Request.toy(Get, "/a/42/c")).body)
            assertEquals(NotFound, handler(Request.toy(Get, "/foo/42/bar")).status)
            assertEquals(NotFound, handler(Request.toy(Get, "/foo/qux/bar")).status)
            assertEquals(NotFound, handler(Request.toy(Get, "/foo/bar")).status)
            assertEquals(NotFound, handler(Request.toy(Get, "/a/b")).status)
            assertEquals(NotFound, handler(Request.toy(Get, "/")).status)
        }
    }

    // Not yet implemented

    // @Test
    // fun testSubSegmentParams() {
    //     run {
    //         val handler = SafeRouter {
    //             get("/<file>.txt", fun(file: String): Handler = { Response().text(file) })
    //         }.handler()
    //         assertEquals(ResponseBody.String("foo"), handler(Request.toy(Get, "/foo.txt")).body)
    //         assertEquals(NotFound, handler(Request.toy(Get, "/bar/foo.txt")).status)
    //         assertEquals(NotFound, handler(Request.toy(Get, "/foo/foo")).status)
    //         assertEquals(NotFound, handler(Request.toy(Get, "/footxt")).status)
    //     }
    // }
}


