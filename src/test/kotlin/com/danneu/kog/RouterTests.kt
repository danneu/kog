package com.danneu.kog

import org.junit.Assert.*
import org.junit.Test
import java.util.Stack


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
    @Test
    fun testEmpty() {
        val router = Router {}
        val response = router(Request.toy(method = Method.Get, path = "/"))
        assertEquals("empty router 404s", Status.NotFound, response.status)
    }

    @Test
    fun hitsOnlyRoute() {
        val router = Router { get("/") { Response().text("ok")} }
        val response = router(Request.toy(method = Method.Get, path = "/"))
        assertEquals("is 200", Status.Ok, response.status)
        assertEquals("has right body", "ok", (response.body as ResponseBody.String).body)
    }

    @Test
    fun missesOnlyRoute() {
        val router = Router { get("/") { Response().text("ok")} }
        val response = router(Request.toy(method = Method.Post, path = "/"))
        assertEquals("is 404", Status.NotFound, response.status)
    }

    @Test
    fun beforewareWorks() {
        val router = Router {
            use { handler -> { req ->
                req.path = "/alt"
                handler(req)
            }}
            get("/") { Response().text("ok")}
            get("/alt") { Response().text("alt")}
        }
        val response = router(Request.toy())
        assertEquals("middleware re-routes request to /alt", "alt", (response.body as ResponseBody.String).body)
    }

    @Test
    fun afterwareWorks() {
        val router = Router {
            use { handler -> { req ->
                val response = handler(req)
                response.setHeader(Header.Custom("test"), "x")
            }}
            get("/") { Response().text("ok")}
        }
        val response = router(Request.toy())
        assertEquals("middleware sets header", "x", response.getHeader(Header.Custom("test")))
    }

    @Test
    fun groupWorks() {
        val router = Router {
            get("/") { Response().text("ok")}
            group("/nest") {
                get("/") { Response().text("nest root") }
            }
        }
        val response = router(Request.toy(path = "/nest"))
        assertEquals("request hits nest", "nest root", (response.body as ResponseBody.String).body)
    }

    @Test
    fun groupOnlyMiddleware() {
        val tokens = Tokenware()
        val router = Router {
            group("/nest") {
                use { handler -> { req ->
                    tokens.add("A")
                    handler(req)
                }}
                get("/") { Response().text("nest root") }
            }
            use { handler -> { req ->
                tokens.add("B")
                handler(req)
            }}
            get("/outside") { Response().text("ok")}
        }
        router(Request.toy(path = "/nest"))
        assertEquals("middleware fires in group", listOf("A"), tokens.list())

        router(Request.toy(path = "/outside"))
        assertEquals("outer route hits middleware outside of group", listOf("A", "B"), tokens.list())
    }

    @Test
    fun hitsManyMiddleware() {
        val tokens = Tokenware()
        val router = Router {
            use { handler -> { req -> tokens.add("A"); handler(req) }}
            use { handler -> { req -> tokens.add("B"); handler(req) }}
            use { handler -> { req -> tokens.add("C"); handler(req) }}
            get("/") { Response() }
            use { handler -> { req -> tokens.add("D"); handler(req) }}
        }
        router(Request.toy(path = "/"))
        assertEquals("hits multiple middleware before short-circuiting", listOf("A", "B", "C"), tokens.list())

        tokens.clear()
        router(Request.toy(path = "/not-found"))
        assertTrue("404 hits nothing", tokens.isEmpty())
    }

    @Test
    fun deepGroup() {
        val router = Router {
            group("/a") {
                group("/b") {
                    group("/c") {
                        get("/d") { Response() }
                    }
                }
            }
        }
        val response = router(Request.toy(path = "/a/b/c/d"))
        assertEquals("request can hit deep nesting", Status.Ok, response.status)
    }

    @Test
    fun otherMethods() {
        val router = Router {
            post("/a/b/c") { Response() }
            options("/d/e/f") { Response() }
        }
        val response1 = router(Request.toy(method = Method.Post,path = "/a/b/c"))
        assertEquals("POST works", Status.Ok, response1.status)

        val response2 = router(Request.toy(method = Method.Options,path = "/d/e/f"))
        assertEquals("OPTIONS works", Status.Ok, response2.status)
    }

    @Test
    fun varargRouteMiddleware() {
        val tokens = Tokenware()
        val router = Router {
            get("/", tokens.mw("A"), tokens.mw("B")) { Response() }
        }
        val response = router(Request.toy())
        assertEquals("all array middleware run", listOf("A", "B"), tokens.list())
    }

    @Test
    fun varargGroupMiddleware() {
        val tokens = Tokenware()
        val router = Router {
            group("/", tokens.mw("A"), tokens.mw("B")) {
                use(tokens.mw("C"), tokens.mw("D"))
                get("/") { Response() }
            }
            get("/after") { Response() }
        }

        val response1 = router(Request.toy(path = "/after"))
        assertEquals("route is hit", Status.Ok, response1.status)
        assertEquals("no middleware was hit", listOf<String>(), tokens.list())

        val response2 = router(Request.toy())
        assertEquals("route is hit", Status.Ok, response2.status)
        assertEquals("all array middleware run", listOf("A", "B", "C", "D"), tokens.list())
    }


    // TRAILING SLASH


    @Test
    fun routeCanHaveTrailingSlash() {
        val router = Router {
            get("/no-trailing") { Response().text(it.path) }
            get("/trailing/") { Response().text(it.path) }
        }

        val response1 = router(Request.toy(path = "/no-trailing"))
        assertEquals("non-trailing slash hit", "/no-trailing", response1.body.toString())

        val response2 = router(Request.toy(path = "/trailing/"))
        assertEquals("trailing slash hit", "/trailing/", response2.body.toString())
    }


    @Test
    fun groupCanHaveTrailingSlash() {
        val router = Router {
            group("/a/") {
                get("/") { Response().text(it.path) }
            }
            group("/b") {
                get("/") { Response().text(it.path) }
            }
        }

        run {
            val r1 = router(Request.toy(path = "/a/"))
            assertEquals("trailing slash hit", "/a/", r1.body.toString())
            val r2 = router(Request.toy(path = "/a"))
            assertEquals("404s when it should", Status.NotFound, r2.status)
        }
        run {
            val r1 = router(Request.toy(path = "/b"))
            assertEquals("non-slash still works", "/b", r1.body.toString())
            val r2 = router(Request.toy(path = "/b/"))
            assertEquals("404s when it should", Status.NotFound, r2.status)
        }
    }

    @Test
    fun nestedRoutesCanHaveTrailingSlash() {
        val router = Router {
            group("/a") {
                group("/b/") {
                    get("/") { Response() }
                }
            }
        }

        val r1 = router(Request.toy(path = "/a/b/"))
        assertEquals("trailing slash hit", Status.Ok, r1.status)
        val r2 = router(Request.toy(path = "/a/b"))
        assertEquals("404s when it should", Status.NotFound, r2.status)
    }


    // HEAD


    @Test
    fun headGetsRoutedAsGet() {
        val router = Router {
            get("/") { Response().text("homepage") }
        }

        run {
            val res = router(Request.toy(method = Method.Head))
            assertEquals("HEAD routed as GET", "homepage", res.body.toString())
        }

        run {
            val res = router(Request.toy(method = Method.Head, path = "/not-found"))
            assertEquals("routes 404", Status.NotFound, res.status)
        }
    }
}



