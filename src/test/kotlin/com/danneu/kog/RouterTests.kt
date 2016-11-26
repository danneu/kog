package com.danneu.kog

import org.junit.Assert.*
import org.junit.Test
import java.util.Stack


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
        val response = router(Request.toy(method = Method.get, path = "/"))
        assertTrue("empty router 404s", response.status == Status.notFound)
    }

    @Test
    fun hitsOnlyRoute() {
        val router = Router { get("/") { Response().text("ok")} }
        val response = router(Request.toy(method = Method.get, path = "/"))
        assertTrue("is 200", response.status == Status.ok)
        assertTrue("has right body", (response.body as ResponseBody.String).body == "ok")
    }

    @Test
    fun missesOnlyRoute() {
        val router = Router { get("/") { Response().text("ok")} }
        val response = router(Request.toy(method = Method.post, path = "/"))
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
                response.setHeader("test", "x")
            }}
            get("/") { Response().text("ok")}
        }
        val response = router(Request.toy())
        assertTrue("middleware sets header", response.getHeader("test") == "x")
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
        val response1 = router(Request.toy(method = Method.post,path = "/a/b/c"))
        assertTrue("POST works", response1.status == Status.ok)

        val response2 = router(Request.toy(method = Method.options,path = "/d/e/f"))
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
}



