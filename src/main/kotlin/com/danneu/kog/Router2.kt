package com.danneu.kog

import java.text.NumberFormat
import java.text.ParseException

/**
 * A crude work-in-progress rewrite of the original kog.Router for url-param
 * support.
 *
 * TODO: Support websockets
 * TODO: Make more type-safe
 * TODO: Support nested routers
 * TODO: Support middleware mounting, .use(). Right now it only supports route-level middleware.
 * TODO: Create tests
 *
 * Will eventually be renamed to replace kog.Router.
 *
 * NOTE: This exists in such an unfinished state because I use kog for fun on some toy projects in production
 * and URL params are a more important than whatever else the other router implements. For this reason, Router2
 * isn't yet documented.
 */

class Router2(vararg mws: Middleware, block: Router2.() -> Router2) {
    var routes: MutableList<Route> = mutableListOf()
    val middleware = composeMiddleware(*mws)

    init {
        block(this)
    }

    fun get(path: Path = Path.root, vararg mws: Middleware, handler: Handler): Router2 = this.apply { routes.add(Route(Method.Get, path, com.danneu.kog.composeMiddleware(*mws)(handler))) }
    fun put(path: Path = Path.root, vararg mws: Middleware, handler: Handler): Router2 = this.apply { routes.add(Route(Method.Put, path, com.danneu.kog.composeMiddleware(*mws)(handler))) }
    fun post(path: Path = Path.root, vararg mws: Middleware, handler: Handler): Router2 = this.apply { routes.add(Route(Method.Post, path, com.danneu.kog.composeMiddleware(*mws)(handler))) }
    fun head(path: Path = Path.root, vararg mws: Middleware, handler: Handler): Router2 = this.apply { routes.add(Route(Method.Head, path, com.danneu.kog.composeMiddleware(*mws)(handler))) }
    fun patch(path: Path = Path.root, vararg mws: Middleware, handler: Handler): Router2 = this.apply { routes.add(Route(Method.Patch, path, com.danneu.kog.composeMiddleware(*mws)(handler))) }
    fun delete(path: Path = Path.root, vararg mws: Middleware, handler: Handler): Router2 = this.apply { routes.add(Route(Method.Delete, path, com.danneu.kog.composeMiddleware(*mws)(handler))) }
    fun options(path: Path = Path.root, vararg mws: Middleware, handler: Handler): Router2 = this.apply { routes.add(Route(Method.Options, path, com.danneu.kog.composeMiddleware(*mws)(handler))) }

    fun handler(): Handler = middleware(fun(req: Request): Response {
        val route = routes.find { it.matches(req.path) } ?: return Response.notFound()
        req.params.putAll(route.params(req.path))
        return route.handler(req)
    })
}

// Allow class reopening so user can define Path extensions
//
// FIXME: I'm parsing in matches() and then again in coerce(). Ideally there'd be just one parse.
abstract class Segment(val name: kotlin.String) {
    abstract fun matches(part: kotlin.String): Boolean
    // Guaranteed to run after matches(), so can assume the coercion will succeed.
    abstract fun coerce(part: kotlin.String): Any

    class Static(name: kotlin.String) : Segment(name) {
        override fun matches(part: kotlin.String) = name == part
        override fun coerce(part: kotlin.String) = part
        override fun toString(): kotlin.String = "Static(\"$name\")"
    }

    class Int(name: kotlin.String) : Segment(name) {
        override fun matches(part: kotlin.String): Boolean = try {
            part.toInt().let { true }
        } catch(e: NumberFormatException) { false }
        override fun coerce(part: kotlin.String) = part.toInt()
        override fun toString(): kotlin.String = "Int"
    }

    // Matches an int at the start of a string, e.g. "42-abc" -> 42
    //
    // Note: NumberFormat.getInstance().parse("9999999999999999999999999999999999999").toInt() == 2147483647
    class IntPrefix(name: kotlin.String) : Segment(name) {
        override fun matches(part: kotlin.String): Boolean = try {
            NumberFormat.getInstance().parse(part).toInt().let { true }
        } catch(e: ParseException) { false }
        override fun coerce(part: kotlin.String) = NumberFormat.getInstance().parse(part).toInt()
        override fun toString(): kotlin.String = "IntPrefix"
    }

    class String(name: kotlin.String) : Segment(name) {
        override fun matches(part: kotlin.String): Boolean = true
        override fun coerce(part: kotlin.String) = part
        override fun toString(): kotlin.String = "String"
    }
}

class Route(val method: Method, val segments: List<Segment>, val handler: Handler) {
    constructor(method: Method, path: Path, handler: Handler): this(method, path.segments, handler)

    fun matches(parts: List<String>): Boolean {
        if (parts.size != segments.size) return false
        return parts.zip(segments).all { (part, segment) -> segment.matches(part) }
    }

    fun matches(path: String): Boolean {
        return matches(toParts(path))
    }

    fun params(path: String): List<Pair<String, Any>> {
        val parts = toParts(path)
        return parts
            .zip(segments)
            // ignore static segments
            .filter { (_, segment) -> segment !is Segment.Static }
            .map { (part, segment) -> segment.name to segment.coerce(part) }
    }
}

fun toParts(path: String): List<String> {
    return path.split("/").filter(String::isNotBlank)
}

// TODO: Consider DRYing up Segment/Path
class Path(val segments: List<Segment>) {
    fun static(name: String): Path = Path(segments.plus(Segment.Static(name)))
    fun int(name: String): Path = Path(segments.plus(Segment.Int(name)))
    fun intPrefix(name: String): Path = Path(segments.plus(Segment.IntPrefix(name)))
    fun string(name: String): Path = Path(segments.plus(Segment.String(name)))
    companion object {
        val root = Path(emptyList())
        fun static(name: String) = Path(listOf(Segment.Static(name)))
        fun int(name: String) = Path(listOf(Segment.Int(name)))
        fun intPrefix(name: String) = Path(listOf(Segment.IntPrefix(name)))
        fun string(name: String) = Path(listOf(Segment.String(name)))
    }
}

fun main(args: Array<String>) {
    fun mw(name: Any): Middleware = { handler -> { req ->
        println("[enter] Middleware $name")
        val res = handler(req)
        println("[exit] Middleware $name")
        res
    }}

    val router = Router2(mw("a"), mw("b")) {
        get { req ->
           Response().text("homepage")
        }
        get(Path.static("stories").intPrefix("id")) { req ->
            val id = req.params["id"] as Int
            Response().text("Story $id")
        }
        get(Path.static("users").int("id"), mw(1), mw(2)) { req ->
            val id = req.params["id"] as Int
            Response().text("hi user: ${id + 100}")
        }
    }

    Server(router.handler()).listen(3001)
}
