package com.danneu.kog

import java.text.NumberFormat
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.createType
import kotlin.reflect.jvm.reflect
import kotlin.reflect.valueParameters

// TODO: Spit out warnings when routes are found that cannot match their recv handlers.

/** Applies `process` to each iterable item until it returns non-null, returning the result of the application.
 *
 */
fun <A, B: Any> Iterable<A>.firstNotNull(process: (A) -> B?): B? = asSequence().firstNotNull(process)
fun <A, B: Any> Sequence<A>.firstNotNull(process: (A) -> B?): B? = mapNotNull { process(it) }.firstOrNull()

fun String.isParam(): Boolean = startsWith("<") && endsWith(">")
fun String.segments(): List<String> = split("/").filter(String::isNotEmpty)

/** A template is a pattern with potential <wildcard> params and a list of types that the handler expects.
 *
 * The template gets applied to a request path and returns a list of values that will get passed into the handler.
 */
class Template(val pattern: String, val types: List<KType> = emptyList()) {
    // Returns null if no match.
    //
    // Note: Below, `return null` short-circuits the function, `null` doesn't. I don't like this code.
    fun extractValues(reqPath: String): List<Any>? {
        val segments1 = pattern.segments()
        val segments2 = reqPath.segments()

        // Bail if trailing slash is different
        if (pattern.endsWith("/") && !reqPath.endsWith("/")) return null
        if (!pattern.endsWith("/") && reqPath.endsWith("/")) return null

        // Route doesn't match if it has a different number of segments than request path
        // e.g. "/a/b/<id>" vs "/a/b"
        if (segments1.size != segments2.size) return null

        // Route doesn't match if its handler specifies a different number of arguments than path params
        // e.g. "/faq" vs fun(username: String)
        if (segments1.filter { it.isParam() }.size != types.size) return null

        var paramIdx = 0

        return segments1.zip(segments2).mapNotNull { (a, b) ->
            if (a.isParam()) {
                val type = types[paramIdx++]
                when (type) {
                    kotlin.Int::class.createType() ->
                        // Note: Will get set to max integer if it exceeds max integer
                        if (Regex("""^\d+$""").matches(b)) {
                            NumberFormat.getInstance().parse(b).toInt()
                        } else {
                            return null
                        }
                    kotlin.String::class.createType() ->
                        b
                    else ->
                        return null
                }
            } else if (a != b) {
                // If we get here, we are comparing static segments, like "foo" != "bar" in "/a/foo" vs "/a/bar"
                // and short-circuit function execution
                return null
            } else {
                // If we get here, the static segments matched, so we continue on to the next .all() iteration.
                null
            }
        }
    }
}

class Route3(val method: Method, val pattern: String, val middleware: Middleware = identity, val recv: Function<Handler>) {
    constructor(method: Method, pattern: String, recv: Function<Handler>):
        this(method, pattern, { x -> x }, recv)

    constructor(method: Method, pattern: String, wares: Array<Middleware>, recv: Function<Handler>):
        this(method, pattern, composeMiddleware(*wares), recv)

    val template = run {
        val params: List<KParameter> = recv.reflect()?.valueParameters!!
        val types: List<KType> = params.map { it.type }
        Template(pattern, types)
    }

    fun run(req: Request): Response? {
        // Bail if method doesn't match the request
        if (method != req.method) return null

        // FIXME: Brittle hackjob
        val values = template.extractValues(req.path) ?: return null
        // javaPrimitiveType seemed to fix an issue I had where arrayOf(kotlin.Int::class.java)
        // worked in getMethod (returns [int]), but `values.map { it::class.java }` didn't work (returns
        // array<class<java.lang.Integer>)
        val classes = values.map { it::class.javaPrimitiveType ?: it.javaClass }
        val method: java.lang.reflect.Method = recv.javaClass.getMethod("invoke", *classes.toTypedArray())
        val handler = method.invoke(recv, *values.toTypedArray()) as Handler
        return middleware(handler)(req)
    }
}


class SafeRouter(vararg wares: Middleware, block: SafeRouter.() -> Unit) {
    var routes = mutableListOf<Route3>()
    var middleware = composeMiddleware(*wares)

    init {
        block(this)
    }

    // TODO: Come up with a better way to DRY this up, perhaps in upstream constructor.
    // TODO: Add the rest of the http verbs
    fun get(pattern: String, recv: Function<Handler>) = routes.add(Route3(Method.Get, pattern, recv))
    fun get(pattern: String, wares: List<Middleware> = emptyList(), recv: Function<Handler>) = routes.add(Route3(Method.Get, pattern, composeMiddleware(*wares.toTypedArray()), recv))
    fun put(pattern: String, recv: Function<Handler>) = routes.add(Route3(Method.Put, pattern, recv))
    fun put(pattern: String, wares: List<Middleware> = emptyList(), recv: Function<Handler>) = routes.add(Route3(Method.Put, pattern, composeMiddleware(*wares.toTypedArray()), recv))
    fun post(pattern: String, recv: Function<Handler>) = routes.add(Route3(Method.Post, pattern, recv))
    fun post(pattern: String, wares: List<Middleware> = emptyList(), recv: Function<Handler>) = routes.add(Route3(Method.Post, pattern, composeMiddleware(*wares.toTypedArray()), recv))
    fun delete(pattern: String, recv: Function<Handler>) = routes.add(Route3(Method.Delete, pattern, recv))
    fun delete(pattern: String, wares: List<Middleware> = emptyList(), recv: Function<Handler>) = routes.add(Route3(Method.Delete, pattern, composeMiddleware(*wares.toTypedArray()), recv))
    fun patch(pattern: String, recv: Function<Handler>) = routes.add(Route3(Method.Patch, pattern, recv))
    fun patch(pattern: String, wares: List<Middleware> = emptyList(), recv: Function<Handler>) = routes.add(Route3(Method.Patch, pattern, composeMiddleware(*wares.toTypedArray()), recv))
    fun head(pattern: String, recv: Function<Handler>) = routes.add(Route3(Method.Head, pattern, recv))
    fun head(pattern: String, wares: List<Middleware> = emptyList(), recv: Function<Handler>) = routes.add(Route3(Method.Head, pattern, composeMiddleware(*wares.toTypedArray()), recv))
    fun options(pattern: String, recv: Function<Handler>) = routes.add(Route3(Method.Head, pattern, recv))
    fun options(pattern: String, wares: List<Middleware> = emptyList(), recv: Function<Handler>) = routes.add(Route3(Method.Head, pattern, composeMiddleware(*wares.toTypedArray()), recv))

    fun handler(): Handler = middleware { req ->
        routes.firstNotNull { route -> route.run(req) } ?: Response.notFound()
    }
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
