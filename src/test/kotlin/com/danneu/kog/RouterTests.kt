package com.danneu.kog

import com.danneu.kog.Method.Delete
import com.danneu.kog.Method.Get
import com.danneu.kog.Method.Post
import com.danneu.kog.Method.Put
import com.danneu.kog.Status.Ok
import com.danneu.kog.Status.NotFound
import org.junit.Assert.*
import org.junit.Test
import java.util.Stack
import java.util.UUID


// generates middleware for testing
class Tokenware {
    private val stack = Stack<String>()

    fun list() = stack.toList()
    fun clear() = stack.clear()
    fun isEmpty() = stack.isEmpty()
    fun add(string: String) { stack.push(string) }

    // returns middleware that mutates the stack
    fun mw(string: String): Middleware {
        return { handler -> { request ->
            stack.push(string)
            handler(request)
        }}
    }
}

class RouterTests {
    // TODO: Add these tests back. Overhauled Router.

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
            val router = Router { get("/foo", fun(): Handler = { Response() }) }
            assertEquals(Ok, router.handler()(Request.toy(Get, "/foo")).status)
        }
        run {
            val router = Router { get("/<foo>", fun(_: String): Handler = { Response() }) }
            assertEquals(Ok, router.handler()(Request.toy(Get, "/foo")).status)
            assertEquals(Ok, router.handler()(Request.toy(Get, "/bar")).status)
            assertEquals(Ok, router.handler()(Request.toy(Get, "/42")).status)
        }
        run {
            val router = Router { get("/<foo>/<bar>", fun(_: String, _: Int): Handler = { Response() }) }
            assertEquals(Ok,       router.handler()(Request.toy(Get, "/a/42")).status)
            assertEquals(NotFound, router.handler()(Request.toy(Get, "/42/a")).status)
            assertEquals(Ok,       router.handler()(Request.toy(Get, "/42/42")).status)
            assertEquals(NotFound, router.handler()(Request.toy(Get, "/a/b")).status)
            assertEquals(NotFound, router.handler()(Request.toy(Get, "/a/b/c")).status)
        }
        run {
            val router = Router {
                get("/a", fun(): Handler = { Response().text("first") })
                get("/a", fun(): Handler = { Response().text("second") })
                get("/a", fun(): Handler = { Response().text("third") })
            }
            assertEquals("matches first route", ResponseBody.String("first"), router.handler()(Request.toy(Get, "/a")).body)
        }
        run {
            val router = Router {
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
            val router = Router { get("/<foo>", fun(): Handler = { Response() }) }
            assertEquals("does not match when recv expects no params", NotFound, router.handler()(Request.toy(Get, "/foo")).status)
        }
        run {
            val handler = Router { get("/foo", fun(): Handler = { Response() }) }.handler()
            assertEquals(NotFound, handler(Request.toy(Post, "/foo")).status)
            assertEquals(NotFound, handler(Request.toy(Delete, "/foo")).status)
            assertEquals(NotFound, handler(Request.toy(Get, "/foo/bar")).status)
            assertEquals(NotFound, handler(Request.toy(Get, "/bar")).status)
        }
        run {
            val handler = Router { get("/<id>", fun(_: String): Handler = { Response() }) }.handler()
            assertEquals(NotFound, handler(Request.toy(Post, "/foo")).status)
            assertEquals(NotFound, handler(Request.toy(Delete, "/foo")).status)
            assertEquals(NotFound, handler(Request.toy(Get, "/foo/bar")).status)
        }
        run {
            val handler = Router { get("/<a>/<b>", fun(_: String, _: String): Handler = { Response() }) }.handler()
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
            val handler = Router { get("/a/b", fun(): Handler = { Response() }) }.handler()
            assertEquals(Ok, handler(Request.toy(Get, "/a/b")).status)
            assertEquals(NotFound, handler(Request.toy(Get, "/a/b/")).status)
            assertEquals(NotFound, handler(Request.toy(Get, "/a/b/c")).status)
            assertEquals(NotFound, handler(Request.toy(Delete, "/a/b")).status)
        }
        run {
            val handler = Router { get("/<a>/<b>", fun(_: String, _: String): Handler = { Response() }) }.handler()
            assertEquals(Ok, handler(Request.toy(Get, "/a/b")).status)
            assertEquals(NotFound, handler(Request.toy(Get, "/a/b/")).status)
        }
    }

    @Test
    fun testParams() {
        run {
            // String
            val handler = Router { get("/<a>", fun(a: String): Handler = { Response().text(a) }) }.handler()
            assertEquals(ResponseBody.String("a"), handler(Request.toy(Get, "/a")).body)
            assertEquals(ResponseBody.String("foo"), handler(Request.toy(Get, "/foo")).body)
        }
        run {
            // Int
            val handler = Router { get("/<a>", fun(a: Int): Handler = { Response().text(a.toString()) }) }.handler()
            assertEquals(ResponseBody.String("42"), handler(Request.toy(Get, "/42")).body)
            assertEquals(ResponseBody.String("0"), handler(Request.toy(Get, "/000")).body)
        }
        run {
            // Int, Int
            val handler = Router {
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
            val handler = Router {
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
            val handler = Router {
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
            val handler = Router {
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
            val handler = Router {
                group {
                    get("/<a>", fun(_: Int): Handler = { Response().text("a") })
                }
                get("/<b>", fun(_: Int): Handler = { Response().text("b") })
            }.handler()

            assertEquals("hits group when match", ResponseBody.String("a"), handler(Request.toy(Get, "/42")).body)
        }

        run {
            val handler = Router {
                group {
                    get("/<a>", fun(_: UUID): Handler = { Response().text("a") })
                }
                get("/<b>", fun(_: Int): Handler = { Response().text("b") })
            }.handler()
            assertEquals("skips group when not match", ResponseBody.String("b"), handler(Request.toy(Get, "/42")).body)
        }

        run {
            val handler = Router {
                group("/nested") {
                    get("", fun(): Handler = { Response().text("a") })
                    get("/", fun(): Handler = { Response().text("b") })
                }
            }.handler()

            assertEquals("empty path means no trailing slash", ResponseBody.String("a"), handler(Request.toy(path = "/nested")).body)
            assertEquals("supports trailing slash", ResponseBody.String("b"), handler(Request.toy(path = "/nested/")).body)
        }
    }


    @Test
    fun testDeepGroup() {
        val router = Router {
            group("/a") {
                group("/b") {
                    group("/c") {
                        get("/d", fun(): Handler = { Response() })
                    }
                }
            }
        }

        run {
            val response = router.handler()(Request.toy(path = "/a/b/c/d"))
            assertEquals("request can hit deep nesting", Status.Ok, response.status)
        }
    }

    @Test
    fun testMountingWithoutPrefix() {
        val router = Router {
            get("/a", fun(): Handler = { Response().text("a") })
            mount(Router {
                get("/b", fun(): Handler = { Response().text("b") })
                group("/c") {
                    get("", fun(): Handler = { Response().text("c") })
                }
            })
        }

        run {
            val response = router.handler()(Request.toy(path = "/a"))
            assertEquals("hits parent router routes", ResponseBody.String("a"), response.body)
        }

        run {
            val response = router.handler()(Request.toy(path = "/b"))
            assertEquals("hits mounted router routes", ResponseBody.String("b"), response.body)
        }

        run {
            val response = router.handler()(Request.toy(path = "/c"))
            assertEquals("hits mounted router groups", ResponseBody.String("c"), response.body)
        }
    }

    @Test
    fun testMountingWithPrefix() {
        val router = Router {
            mount("/prefix", Router {
                get("/b", fun(): Handler = { Response().text("b") })
                group("/c") {
                    get("", fun(): Handler = { Response().text("c") })
                }
            })
        }

        run {
            val response = router.handler()(Request.toy(path = "/b"))
            assertEquals("unprefixed mounted routes do not match", Status.NotFound, response.status)
        }

        run {
            val response = router.handler()(Request.toy(path = "/prefix/b"))
            assertEquals("hits mounted router routes", ResponseBody.String("b"), response.body)
        }

        run {
            val response = router.handler()(Request.toy(path = "/prefix/c"))
            assertEquals("hits mounted router groups", ResponseBody.String("c"), response.body)
        }
    }

    @Test
    fun testMountingInGroup() {
        val router = Router {
            group("/group") {
                mount(Router {
                    get("/a", fun(): Handler = { Response().text("a") })
                })
                mount("/prefix", Router {
                    get("/b", fun(): Handler = { Response().text("b") })
                })
            }
        }

        run {
            val response = router.handler()(Request.toy(path = "/group/a"))
            assertEquals("hits grouped unprefixed route", ResponseBody.String("a"), response.body)
        }

        run {
            val response = router.handler()(Request.toy(path = "/group/prefix/b"))
            assertEquals("hits grouped prefixed route", ResponseBody.String("b"), response.body)
        }
    }


    // MIDDLEWARE


    @Test
    fun testGroupOnlyMiddleware() {
        val tokens = Tokenware()

        val ware1: Middleware = { handler -> { request ->
            tokens.add("A")
            handler(request)
        }}

        val ware2: Middleware = { handler -> { request ->
            tokens.add("B")
            handler(request)
        }}

        val handler = Router {
            group("/nested", listOf(ware1)) {
                get("", fun(): Handler = { Response().text("nest root") })
            }
            get("/outside", listOf(ware2), fun(): Handler = { Response().text("ok") })
        }.handler()

        run {
            handler(Request.toy(path = "/nested"))
            assertEquals("middleware fires in group", listOf("A"), tokens.list())
        }

        run {
            handler(Request.toy(path = "/outside"))
            assertEquals("outer route hits middleware outside of group", listOf("A", "B"), tokens.list())
        }

        run {
            handler(Request.toy(path = "/not-found"))
            assertEquals("404 hits nothing", listOf("A", "B"), tokens.list())
        }
    }


    @Test
    fun testBeforeware() {
        val beforeware: Middleware = { handler -> { req ->
            req.path = "/alt"
            handler(req)
        }}
        val handler = Router(beforeware) {
            get("/", fun(): Handler = { Response().text("ok") })
            get("/alt", fun(): Handler = { Response().text("alt") })
        }.handler()

        val response = handler(Request.toy())
        assertEquals("middleware re-routes request to /alt", "alt", (response.body as ResponseBody.String).body)
    }

    @Test
    fun testAfterware() {
        val afterware: Middleware = { handler -> { request ->
            val response = handler(request)
            response.setHeader(Header.Custom("test"), "x")
        }}
        val handler = Router(afterware) {
            get("/", fun(): Handler = { Response().text("ok") })
        }.handler()

        val response = handler(Request.toy())
        assertEquals("middleware sets header", "x", response.getHeader(Header.Custom("test")))
    }

    // Not yet implemented

    // @Test
    // fun testSubSegmentParams() {
    //     run {
    //         val handler = Router {
    //             get("/<file>.txt", fun(file: String): Handler = { Response().text(file) })
    //         }.handler()
    //         assertEquals(ResponseBody.String("foo"), handler(Request.toy(Get, "/foo.txt")).body)
    //         assertEquals(NotFound, handler(Request.toy(Get, "/bar/foo.txt")).status)
    //         assertEquals(NotFound, handler(Request.toy(Get, "/foo/foo")).status)
    //         assertEquals(NotFound, handler(Request.toy(Get, "/footxt")).status)
    //     }
    // }
}


