package com.danneu.kog

import org.junit.Assert.*
import org.junit.Test
import java.util.Stack


// TODO: Refactor all assertTrue's into assertEquals since assertEquals prints out expects vs actual values


// generates middleware for testing
class Tokenware() {
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
        assertTrue("empty router 404s", response.status == Status.notFound)
    }

    @Test
    fun hitsOnlyRoute() {
        val router = Router { get("/") { Response().text("ok")} }
        val response = router(Request.toy(method = Method.Get, path = "/"))
        assertTrue("is 200", response.status == Status.ok)
        assertTrue("has right body", (response.body as ResponseBody.String).body == "ok")
    }

    @Test
    fun missesOnlyRoute() {
        val router = Router { get("/") { Response().text("ok")} }
        val response = router(Request.toy(method = Method.Post, path = "/"))
        assertTrue("is 404", response.status == Status.notFound)
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
        assertTrue("middleware re-routes request to /alt", (response.body as ResponseBody.String).body == "alt")
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
        assertTrue("middleware sets header", response.getHeader(Header.Custom("test")) == "x")
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
        assertTrue("request hits nest", (response.body as ResponseBody.String).body == "nest root")
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
        assertTrue("middleware fires in group", tokens.list() == listOf("A"))

        router(Request.toy(path = "/outside"))
        assertTrue("outer route hits middleware outside of group", tokens.list() == listOf("A", "B"))
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
        assertTrue("hits multiple middleware before short-circuiting", tokens.list() == listOf("A", "B", "C"))

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
        assertTrue("request can hit deep nesting", response.status == Status.ok)
    }

    @Test
    fun otherMethods() {
        val router = Router {
            post("/a/b/c") { Response() }
            options("/d/e/f") { Response() }
        }
        val response1 = router(Request.toy(method = Method.Post,path = "/a/b/c"))
        assertTrue("POST works", response1.status == Status.ok)

        val response2 = router(Request.toy(method = Method.Options,path = "/d/e/f"))
        assertTrue("OPTIONS works", response2.status == Status.ok)
    }

    @Test
    fun varargRouteMiddleware() {
        val tokens = Tokenware()
        val router = Router {
            get("/", tokens.mw("A"), tokens.mw("B")) { Response() }
        }
        val response = router(Request.toy())
        assertTrue("all array middleware run", tokens.list() == listOf("A", "B"))
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
        assertTrue("route is hit", response1.status == Status.ok)
        assertTrue("no middleware was hit", tokens.list() == listOf<String>())

        val response2 = router(Request.toy())
        assertTrue("route is hit", response2.status == Status.ok)
        assertTrue("all array middleware run", tokens.list() == listOf("A", "B", "C", "D"))
    }


    // TRAILING SLASH


    @Test
    fun routeCanHaveTrailingSlash() {
        val router = Router {
            get("/no-trailing") { Response().text(it.path) }
            get("/trailing/") { Response().text(it.path) }
        }

        val response1 = router(Request.toy(path = "/no-trailing"))
        assertTrue("non-trailing slash hit", response1.body.toString() == "/no-trailing")

        val response2 = router(Request.toy(path = "/trailing/"))
        assertTrue("trailing slash hit", response2.body.toString() == "/trailing/")
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
            assertTrue("trailing slash hit", r1.body.toString() == "/a/")
            val r2 = router(Request.toy(path = "/a"))
            assertTrue("404s when it should", r2.status == Status.notFound)
        }
        run {
            val r1 = router(Request.toy(path = "/b"))
            println("${r1.status}, ${r1.body}")
            assertTrue("non-slash still works", r1.body.toString() == "/b")
            val r2 = router(Request.toy(path = "/b/"))
            assertTrue("404s when it should", r2.status == Status.notFound)
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
        assertTrue("trailing slash hit", r1.status == Status.ok)
        val r2 = router(Request.toy(path = "/a/b"))
        assertTrue("404s when it should", r2.status == Status.notFound)
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
            assertEquals("routes 404", Status.notFound, res.status)
        }
    }
}



