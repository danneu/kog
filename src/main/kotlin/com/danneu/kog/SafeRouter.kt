package com.danneu.kog.sandbox

import com.danneu.kog.Handler
import com.danneu.kog.Method
import com.danneu.kog.Middleware
import com.danneu.kog.Request
import com.danneu.kog.Response
import com.danneu.kog.Server
import com.danneu.kog.composeMiddleware
import java.text.NumberFormat
import java.util.UUID
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.createType
import kotlin.reflect.jvm.reflect
import kotlin.reflect.valueParameters
import kotlin.text.RegexOption.IGNORE_CASE


// Don't call matcher.find() before this
// Note: starts at 1. group 0 is the whole expr.
fun Matcher.firstMatchingGroupNumber(): Int? {
    if (!this.find()) return null
    // There is a match
    var num = 0
    while (num++ < this.groupCount()) {
        if (this.group(num) != null) return num
    }
    return null
}
fun String.isParam(): Boolean = startsWith("<") && endsWith(">")
fun String.segments(): List<String> = split("/").drop(1)
fun <A> List<A>.valuesAt(indexes: List<Int>): List<A> {
    return this.filterIndexed { idx, _ -> idx in indexes }
}

class Route(val method: Method, val pattern: String, val recv: Function<Handler>, wares: List<Middleware> = emptyList()) {
    val middleware = composeMiddleware(*wares.toTypedArray())
    val types = recv.types()

    val paramIdxs = pattern.segments().mapIndexed { idx, seg ->
        if (seg.isParam()) { idx } else { null }
    }.filterNotNull()

    override fun toString(): String {
        return "Route($method '$pattern' ${types.map{it}} ${paramIdxs})"
    }

    fun toRegex(): Regex? {
        // Route cannot match if type count isn't same as param count
        if (paramIdxs.size != types.size) {
            println("Warning: Route ${this} handler expects different arguments than the provided url pattern")
            return null
        }

        var paramIdx = 0

        return pattern.segments().mapIndexed { idx, segment ->
            if (idx in paramIdxs) {
                val type = types[paramIdx++]
                when (type) {
                    kotlin.Int::class.createType() ->
                        """/[0-9]+"""
                    kotlin.String::class.createType() ->
                        """/[^\/]+"""
                    UUID::class.createType() ->
                        """[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"""
                    else ->
                        ""
                }
            } else {
                // static segment
                "/$segment"
            }
        }.let { Regex("^($method:${it.joinToString("")})$", IGNORE_CASE) }
    }

    fun handle(request: Request): Response {
        val args = request.path.segments().valuesAt(paramIdxs).zip(types).map { (seg, type) ->
            when (type) {
                kotlin.Int::class.createType() -> {
                    NumberFormat.getInstance().parse(seg).toInt()
                }
                kotlin.String::class.createType() -> {
                    seg
                }
                UUID::class.createType() -> {
                    UUID.fromString(seg)
                }
                else -> {
                    throw java.lang.IllegalStateException("Impossible")
                }
            }
        }

        val classes = args.map { it::class.javaPrimitiveType ?: it.javaClass }
        val method: java.lang.reflect.Method = recv.javaClass.getMethod("invoke", *classes.toTypedArray())
        method.isAccessible = true
        val handler = method.invoke(recv, *args.toTypedArray()) as Handler
        return middleware(handler)(request)
    }
}

fun Function<Handler>.types(): List<KType> {
    val params: List<KParameter> = this.reflect()?.valueParameters!!
    val types: List<KType> = params.map { it.type }
    return types
}

class Dispatcher(val routes: List<Route>) {
    val regex: Pattern = routes
        .map(Route::toRegex)
        .map { it ?: Regex("""^(#)$""") } // cannot be matched
        .map(Regex::pattern)
        .joinToString("|")
        .let(Pattern::compile)

    fun matchingRoute(request: Request): Route? {
        val key = "${request.method}:${request.path}"
        val groupNum = regex.matcher(key).firstMatchingGroupNumber() ?: return null
        return routes[groupNum - 1]
    }

    fun handler(): Handler = { request -> matchingRoute(request)?.handle(request) ?: Response.notFound() }

    override fun toString(): String {
        return "Dispatcher(${regex.pattern()})"
    }
}

class SafeRouter(vararg wares: Middleware, block: SafeRouter.() -> Unit) {
    val middleware = composeMiddleware(*wares)
    val routes = mutableListOf<Route>()
    val dispatcher: Dispatcher

    init {
        this.block()
        dispatcher = Dispatcher(routes)
    }

    fun get(pattern: String, recv: Function<Handler>) = routes.add(Route(Method.Get, pattern, recv))
    fun put(pattern: String, recv: Function<Handler>) = routes.add(Route(Method.Put, pattern, recv))
    fun post(pattern: String, recv: Function<Handler>) = routes.add(Route(Method.Post, pattern, recv))
    fun delete(pattern: String, recv: Function<Handler>) = routes.add(Route(Method.Delete, pattern, recv))
    fun patch(pattern: String, recv: Function<Handler>) = routes.add(Route(Method.Patch, pattern, recv))
    fun head(pattern: String, recv: Function<Handler>) = routes.add(Route(Method.Head, pattern, recv))
    fun options(pattern: String, recv: Function<Handler>) = routes.add(Route(Method.Options, pattern, recv))
    fun get(pattern: String, wares: List<Middleware> = emptyList(), recv: Function<Handler>) = routes.add(Route(Method.Get, pattern, recv, wares))
    fun put(pattern: String, wares: List<Middleware> = emptyList(), recv: Function<Handler>) = routes.add(Route(Method.Put, pattern, recv, wares))
    fun post(pattern: String, wares: List<Middleware> = emptyList(), recv: Function<Handler>) = routes.add(Route(Method.Post, pattern, recv, wares))
    fun delete(pattern: String, wares: List<Middleware> = emptyList(), recv: Function<Handler>) = routes.add(Route(Method.Delete, pattern, recv, wares))
    fun patch(pattern: String, wares: List<Middleware> = emptyList(), recv: Function<Handler>) = routes.add(Route(Method.Patch, pattern, recv, wares))
    fun head(pattern: String, wares: List<Middleware> = emptyList(), recv: Function<Handler>) = routes.add(Route(Method.Head, pattern, recv, wares))
    fun options(pattern: String, wares: List<Middleware> = emptyList(), recv: Function<Handler>) = routes.add(Route(Method.Options, pattern, recv, wares))

    fun handler(): Handler = middleware(dispatcher.handler())
}


fun main(args: Array<String>) {
    fun mw(name: String): Middleware = { handler -> { req ->
        println("--> mw $name")
        val response = handler(req)
        println("<-- mw $name")
        response
    }}

    val router = SafeRouter(mw("start1"), mw("start2")) {
        get("/<id>", listOf(mw("a"), mw("b")), fun(id: Int): Handler = {
            Response().text("id is $id")
        })
        get("/stories/<id>", fun(id: Int): Handler = {
            Response().text("show story $id")
        })
        get("/stories/<id>/new", fun(id: Int): Handler = {
            Response().text("new story $id")
        })
        get("/stories/<id>/comments/<id>", fun(storyId: Int, commentId: Int): Handler = {
            Response().text("comment $commentId on story $storyId")
        })
        get("/foo/bar", fun(storyId: Int, commentId: Int): Handler = {
            Response().text("this should 404 since our handler expects more arguments than the route can match")
        })
        get("/foo/bar", fun(): Handler = {
            Response().text("this will match")
        })
    }

    Server(router.handler()).listen(3002)
}
