package com.danneu.kog

import com.danneu.kog.middleware.composeMiddleware
import com.danneu.kog.middleware.identity
import java.util.UUID
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.createType
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.reflect


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
    val middleware = composeMiddleware(wares)
    val types = recv.types()

    val paramIdxs = pattern.segments().mapIndexed { idx, seg ->
        if (seg.isParam()) { idx } else { null }
    }.filterNotNull()

    override fun toString(): String {
        return "Route($method '$pattern' ${types.map{it}} $paramIdxs)"
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
                val ktype = types[paramIdx++]
                when (ktype) {
                    kotlin.Int::class.createType() ->
                        """/-?[0-9]+"""
                    kotlin.Long::class.createType() ->
                        """/-?[0-9]+"""
                    kotlin.Float::class.createType() ->
                        """/-?[0-9]*\.?[0-9]*"""
                    kotlin.Double::class.createType() ->
                        """/-?[0-9]*\.?[0-9]*"""
                    kotlin.String::class.createType() ->
                        """/[^\/]+"""
                    UUID::class.createType() ->
                        """/[0-9A-Fa-f]{8}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{12}"""
                    else ->
                        ""
                }
            } else {
                // static segment
                "/$segment"
            }
        }.let { regexStrings ->
            Regex("^($method:${regexStrings.joinToString("")})$")
        }
    }

    fun handle(request: Request): Response {
        val args = request.path.segments().valuesAt(paramIdxs).zip(types).map { (seg: String, ktype: KType) ->
            when (ktype) {
                kotlin.Int::class.createType() ->
                    //NumberFormat.getInstance().parse(seg).toInt()
                    seg.toIntOrNull() ?: return Response.notFound()
                kotlin.Long::class.createType() ->
                    seg.toLongOrNull() ?: return Response.notFound()
                kotlin.Float::class.createType() ->
                    seg.toFloatOrNull() ?: return Response.notFound()
                kotlin.Double::class.createType() ->
                    seg.toDoubleOrNull() ?: return Response.notFound()
                kotlin.String::class.createType() ->
                    seg
                UUID::class.createType() ->
                    UUID.fromString(seg)
                else ->
                    throw java.lang.IllegalStateException("Impossible")
            }
        }

        val classes = args.map { it::class.javaPrimitiveType ?: it.javaClass }
        val method: java.lang.reflect.Method = recv.javaClass.getMethod("invoke", *classes.toTypedArray())
        method.isAccessible = true
        @Suppress("UNCHECKED_CAST")
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

class SafeRouter(val middleware: Middleware, block: SafeRouter.() -> Unit) {
    @JvmOverloads constructor(wares: List<Middleware> = emptyList(), block: SafeRouter.() -> Unit):
        this(composeMiddleware(wares), block)

    constructor(vararg wares: Middleware, block: SafeRouter.() -> Unit):
        this(composeMiddleware(*wares), block)

    val routes = mutableListOf<Route>()
    val dispatcher: Dispatcher

    init {
        this.block()
        dispatcher = Dispatcher(routes)
    }

    fun get(pattern: String, recv: Function<Handler>) =
        routes.add(Route(Method.Get, pattern, recv))
    fun put(pattern: String, recv: Function<Handler>) =
        routes.add(Route(Method.Put, pattern, recv))
    fun post(pattern: String, recv: Function<Handler>) =
        routes.add(Route(Method.Post, pattern, recv))
    fun delete(pattern: String, recv: Function<Handler>) =
        routes.add(Route(Method.Delete, pattern, recv))
    fun patch(pattern: String, recv: Function<Handler>) =
        routes.add(Route(Method.Patch, pattern, recv))
    fun head(pattern: String, recv: Function<Handler>) =
        routes.add(Route(Method.Head, pattern, recv))
    fun options(pattern: String, recv: Function<Handler>) =
        routes.add(Route(Method.Options, pattern, recv))

    fun get(pattern: String, wares: List<Middleware> = emptyList(), recv: Function<Handler>) =
        routes.add(Route(Method.Get, pattern, recv, wares))
    fun put(pattern: String, wares: List<Middleware> = emptyList(), recv: Function<Handler>) =
        routes.add(Route(Method.Put, pattern, recv, wares))
    fun post(pattern: String, wares: List<Middleware> = emptyList(), recv: Function<Handler>) =
        routes.add(Route(Method.Post, pattern, recv, wares))
    fun delete(pattern: String, wares: List<Middleware> = emptyList(), recv: Function<Handler>) =
        routes.add(Route(Method.Delete, pattern, recv, wares))
    fun patch(pattern: String, wares: List<Middleware> = emptyList(), recv: Function<Handler>) =
        routes.add(Route(Method.Patch, pattern, recv, wares))
    fun head(pattern: String, wares: List<Middleware> = emptyList(), recv: Function<Handler>) =
        routes.add(Route(Method.Head, pattern, recv, wares))
    fun options(pattern: String, wares: List<Middleware> = emptyList(), recv: Function<Handler>) =
        routes.add(Route(Method.Options, pattern, recv, wares))

    fun group(prefixPattern: String, wares: List<Middleware> = emptyList(), block: RouteGroup.() -> Unit) {
        val group = RouteGroup(prefixPattern, composeMiddleware(wares))
        group.block()
        group.routes.forEach { route -> routes.add(route) }
    }

    fun group(wares: List<Middleware> = emptyList(), block: RouteGroup.() -> Unit) {
        group("", wares, block)
    }

    fun group(vararg wares: Middleware, block: RouteGroup.() -> Unit) {
        group("", wares.toList(), block)
    }

    fun handler(): Handler = middleware(dispatcher.handler())
}

// concatPatterns("/", "/a", "//b") => "/a/b"
fun concatPatterns(vararg patterns: String): String {
    return patterns
        .joinToString("")
        .replace(Regex("/{2,}"), "/")
}

class RouteGroup(val prefixPattern: String, val middleware: Middleware = identity) {
    val routes = mutableListOf<Route>()

    fun get(pattern: String, recv: Function<Handler>) =
        routes.add(Route(Method.Get, concatPatterns(prefixPattern, pattern), recv, listOf(middleware)))
    fun get(pattern: String, wares: List<Middleware> = emptyList(), recv: Function<Handler>) =
        routes.add(Route(Method.Get, concatPatterns(prefixPattern, pattern), recv, listOf(middleware).plus(wares)))
    fun put(pattern: String, recv: Function<Handler>) =
        routes.add(Route(Method.Put, concatPatterns(prefixPattern, pattern), recv, listOf(middleware)))
    fun put(pattern: String, wares: List<Middleware> = emptyList(), recv: Function<Handler>) =
        routes.add(Route(Method.Put, concatPatterns(prefixPattern, pattern), recv, listOf(middleware).plus(wares)))
    fun post(pattern: String, recv: Function<Handler>) =
        routes.add(Route(Method.Post, concatPatterns(prefixPattern, pattern), recv, listOf(middleware)))
    fun post(pattern: String, wares: List<Middleware> = emptyList(), recv: Function<Handler>) =
        routes.add(Route(Method.Post, concatPatterns(prefixPattern, pattern), recv, listOf(middleware).plus(wares)))
    fun delete(pattern: String, recv: Function<Handler>) =
        routes.add(Route(Method.Delete, concatPatterns(prefixPattern, pattern), recv, listOf(middleware)))
    fun delete(pattern: String, wares: List<Middleware> = emptyList(), recv: Function<Handler>) =
        routes.add(Route(Method.Delete, concatPatterns(prefixPattern, pattern), recv, listOf(middleware).plus(wares)))
    fun patch(pattern: String, recv: Function<Handler>) =
        routes.add(Route(Method.Patch, concatPatterns(prefixPattern, pattern), recv, listOf(middleware)))
    fun patch(pattern: String, wares: List<Middleware> = emptyList(), recv: Function<Handler>) =
        routes.add(Route(Method.Patch, concatPatterns(prefixPattern, pattern), recv, listOf(middleware).plus(wares)))
    fun head(pattern: String, recv: Function<Handler>) =
        routes.add(Route(Method.Head, concatPatterns(prefixPattern, pattern), recv, listOf(middleware)))
    fun head(pattern: String, wares: List<Middleware> = emptyList(), recv: Function<Handler>) =
        routes.add(Route(Method.Head, concatPatterns(prefixPattern, pattern), recv, listOf(middleware).plus(wares)))
    fun options(pattern: String, recv: Function<Handler>) =
        routes.add(Route(Method.Options, concatPatterns(prefixPattern, pattern), recv, listOf(middleware)))
    fun options(pattern: String, wares: List<Middleware> = emptyList(), recv: Function<Handler>) =
        routes.add(Route(Method.Options, concatPatterns(prefixPattern, pattern), recv, listOf(middleware).plus(wares)))

    fun group(subPrefixPattern: String, wares: List<Middleware> = emptyList(), block: RouteGroup.() -> Unit) {
        val group = RouteGroup(concatPatterns(prefixPattern, subPrefixPattern), composeMiddleware(wares))
        group.block()
        group.routes.forEach { route -> routes.add(route) }
    }

    fun group(wares: List<Middleware> = emptyList(), block: RouteGroup.() -> Unit) {
        this.group("", wares, block)
    }

    fun group(vararg wares: Middleware, block: RouteGroup.() -> Unit) {
        this.group("", wares.toList(), block)
    }
}


//fun main(args: Array<String>) {
//    fun mw(name: String): Middleware = { handler -> { req ->
//        println("--> mw $name")
//        val response = handler(req)
//        println("<-- mw $name")
//        response
//    }}
//
//    val router = SafeRouter(listOf(mw("start1"), mw("start2"))) {
//        get("/<id>", fun(id: Int): Handler = {
//            Response().text("/<id>, id = $id")
//        })
//
//        group("/<id>", listOf(mw("a"))) {
//            get("/new", listOf(mw("b")), fun(id: Int): Handler = { Response().text("/new id is $id") })
//        }
//
//        group {
//            get("/new", fun(): Handler = { Response().text("/new") })
//        }
//
//        get("/<id>", listOf(mw("a"), mw("b")), fun(id: Int): Handler = {
//            Response().text("id is $id")
//        })
//        get("/stories/<id>", fun(id: Int): Handler = {
//            Response().text("show story $id")
//        })
//        get("/stories/<id>/new", fun(id: Int): Handler = {
//            Response().text("new story $id")
//        })
//        get("/stories/<id>/comments/<id>", fun(storyId: Int, commentId: Int): Handler = {
//            Response().text("comment $commentId on story $storyId")
//        })
//        get("/foo/bar", fun(storyId: Int, commentId: Int): Handler = {
//            Response().text("this should 404 since our handler expects more arguments than the route can match")
//        })
//        get("/foo/bar", fun(): Handler = {
//            Response().text("this will match")
//        })
//    }
//
//    Server(router.handler()).listen(3002)
//}
