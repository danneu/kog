package com.danneu.kog

import com.danneu.kog.Method.Delete
import com.danneu.kog.Method.Get
import com.danneu.kog.Method.Post
import com.danneu.kog.Method.Put
import com.danneu.kog.Status.Ok
import com.danneu.kog.Status.NotFound
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

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
            val router = SafeRouter { get("/<foo>", fun(_: String): Handler = { Response() }) }
            assertEquals(Ok, router.handler()(Request.toy(Get, "/foo")).status)
            assertEquals(Ok, router.handler()(Request.toy(Get, "/bar")).status)
            assertEquals(Ok, router.handler()(Request.toy(Get, "/42")).status)
        }
        run {
            val router = SafeRouter { get("/<foo>/<bar>", fun(_: String, _: Int): Handler = { Response() }) }
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
            val handler = SafeRouter { get("/<id>", fun(_: String): Handler = { Response() }) }.handler()
            assertEquals(NotFound, handler(Request.toy(Post, "/foo")).status)
            assertEquals(NotFound, handler(Request.toy(Delete, "/foo")).status)
            assertEquals(NotFound, handler(Request.toy(Get, "/foo/bar")).status)
        }
        run {
            val handler = SafeRouter { get("/<a>/<b>", fun(_: String, _: String): Handler = { Response() }) }.handler()
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
            val handler = SafeRouter { get("/<a>/<b>", fun(_: String, _: String): Handler = { Response() }) }.handler()
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
            assertEquals(ResponseBody.String("-1"), handler(Request.toy(Get, "/1/-2")).body)
            assertEquals(ResponseBody.String("-1"), handler(Request.toy(Get, "/-2/1")).body)
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

    @Test
    fun testUuid() {
        run {
            val handler = SafeRouter {
                get("/<a>", fun(id: UUID): Handler = { Response().text(id.toString()) })
                get("/<a>", fun(_: String): Handler = { Response().text("not-uuid") })
            }.handler()

            val uuid = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
            assertEquals("lowercase uuid hits", ResponseBody.String(uuid), handler(Request.toy(Get, "/$uuid")).body)
            assertEquals("uppercase uuid hits", ResponseBody.String(uuid), handler(Request.toy(Get, "/${uuid.toUpperCase()}")).body)
            assertEquals("skips handler if not uuid", ResponseBody.String("not-uuid"), handler(Request.toy(Get, "/abc")).body)
        }
    }

    @Test
    fun testNumberBounds() {
        run {
            val handler = SafeRouter {
                get("/int/<a>", fun(id: Int): Handler = { Response().text(id.toString()) })
                get("/long/<a>", fun(id: Long): Handler = { Response().text(id.toString()) })
                get("/float/<a>", fun(id: Float): Handler = { Response().text(id.toString()) })
                get("/double/<a>", fun(id: Double): Handler = { Response().text(id.toString()) })
            }.handler()

            assertEquals("max int matches", ResponseBody.String(Int.MAX_VALUE.toString()),
                handler(Request.toy(Get, "/int/${Int.MAX_VALUE}")).body)
            assertEquals("negative int matches", ResponseBody.String("-42"),
                handler(Request.toy(Get, "/int/-42")).body)
            assertEquals("beyond max int does not match", NotFound,
                handler(Request.toy(Get, "/int/${Int.MAX_VALUE + 1L}")).status)
            assertEquals("beyond min int does not match", NotFound,
                handler(Request.toy(Get, "/int/${Int.MIN_VALUE - 1L}")).status)

            assertEquals("max long matches", ResponseBody.String(Long.MAX_VALUE.toString()),
                handler(Request.toy(Get, "/long/${Long.MAX_VALUE}")).body)
            assertEquals("negative long matches", ResponseBody.String("-42"),
                handler(Request.toy(Get, "/long/-42")).body)
            assertEquals("beyond max long does not match", NotFound,
                handler(Request.toy(Get, "/long/${Long.MAX_VALUE}0")).status)
            assertEquals("beyond max long does not match", NotFound,
                handler(Request.toy(Get, "/long/${Long.MIN_VALUE}0")).status)

            // TODO: Test against min/max float and decimal values

            assertEquals("a positive float matches", ResponseBody.String("3.14"), handler(Request.toy(Get, "/float/3.14")).body)
            assertEquals("a negative float matches", ResponseBody.String("-3.14"), handler(Request.toy(Get, "/float/-3.14")).body)

            assertEquals("a positive double matches", ResponseBody.String("3.14"), handler(Request.toy(Get, "/double/3.14")).body)
            assertEquals("a negative double matches", ResponseBody.String("-3.14"), handler(Request.toy(Get, "/double/-3.14")).body)
        }

    }

    @Test
    fun testGroups() {
        run {
            val handler = SafeRouter {
                group {
                    get("/<a>", fun(_: Int): Handler = { Response().text("a") })
                }
                get("/<b>", fun(_: Int): Handler = { Response().text("b") })
            }.handler()

            assertEquals("hits group when match", ResponseBody.String("a"), handler(Request.toy(Get, "/42")).body)
        }

        run {
            val handler = SafeRouter {
                group {
                    get("/<a>", fun(_: UUID): Handler = { Response().text("a") })
                }
                get("/<b>", fun(_: Int): Handler = { Response().text("b") })
            }.handler()
            assertEquals("skips group when not match", ResponseBody.String("b"), handler(Request.toy(Get, "/42")).body)
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


