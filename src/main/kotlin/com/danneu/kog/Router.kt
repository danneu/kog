package com.danneu.kog

import java.util.Stack


// HELPERS


fun toPath(vararg paths: String): String {
    return ("/" + paths
      .filter { it != "/" }
      .joinToString("/")
    ).replace(Regex("/{2,}"), "/")
}


private fun composeHandler(wares: Array<Middleware>): Handler {
    val notFound: Handler = { Response(Status.notFound) }
    return wares.reversed().fold(notFound, { handler: Handler, ware: Middleware -> ware(handler) })
}


data class Route(val method: Method, val path: String, val handler: Handler) {
    fun matches(request: Request): Boolean = request.method == method && request.path == path
    operator fun invoke(request: Request): Response = handler(request)
    override fun toString(): String = "[Route ${method.name} \"$path\"]"

    fun middleware(): Middleware = { handler -> { req ->
        if (matches(req)) {
            this.handler(req)
        } else {
            handler(req)
        }
    }}
}


sealed class StackItem {
    class Route(val route: com.danneu.kog.Route): StackItem()
    class Group(val group: com.danneu.kog.Group): StackItem()
}


class Group(val path: String, mws: Array<out Middleware> = emptyArray(), init: Group.() -> Unit = {}) {
    val routes = Stack<Route>()
    val stack = Stack<StackItem>()
    val wares = Stack<Middleware>()

    init {
        for (mw in mws) {
            use(mw)
        }
        init()
    }

    fun method(method: Method): (String, Middleware, Handler) -> Unit = { path: String, middleware: Middleware, handler: Handler ->
        val route = Route(method, toPath(this.path, path), middleware(handler))
        routes.push(route)
        stack.push(StackItem.Route(route))
        wares.push(route.middleware())
    }

    fun get(path: String, vararg mws: Middleware, handler: Handler) = method(Method.get)(path, composeMiddleware(*mws), handler)
    fun put(path: String, vararg mws: Middleware, handler: Handler) = method(Method.put)(path, composeMiddleware(*mws), handler)
    fun post(path: String, vararg mws: Middleware, handler: Handler) = method(Method.post)(path, composeMiddleware(*mws), handler)
    fun head(path: String, vararg mws: Middleware, handler: Handler) = method(Method.head)(path, composeMiddleware(*mws), handler)
    fun patch(path: String, vararg mws: Middleware, handler: Handler) = method(Method.patch)(path, composeMiddleware(*mws), handler)
    fun delete(path: String, vararg mws: Middleware, handler: Handler) = method(Method.delete)(path, composeMiddleware(*mws), handler)
    fun options(path: String, vararg mws: Middleware, handler: Handler) = method(Method.options)(path, composeMiddleware(*mws), handler)

    fun websocket(path: String, vararg mws: Middleware, accept: WebSocketAcceptor) = method(Method.get)(path, composeMiddleware(*mws), {
        Response.websocket(path, accept)
    })

    fun group(path: String, vararg mws: Middleware, subinit: Group.() -> Unit): Group {
        val group = Group(toPath(this.path, path), mws, subinit)
        stack.push(StackItem.Group(group))
        wares.push({ handler -> { req ->
            if (group.matches(req)) {
                group.handler()(req)
            } else {
                handler(req)
            }
        }})
        return group
    }

    fun handler(): Handler = { req ->
        if (matches(req)) {
            (composeHandler(this.wares.toTypedArray()))(req)
        } else {
            Response(Status.notFound)
        }
    }

    fun use(middleware: Middleware) {
        wares.push(middleware)
    }

    fun use(vararg middlewares: Middleware) {
        for (middleware in middlewares) {
            wares.push(middleware)
        }
    }

    fun matches(request: Request): Boolean {
        return stack.any { item ->
            when (item) {
                is StackItem.Route -> item.route.matches(request)
                is StackItem.Group -> item.group.matches(request)
                else -> false
            }
        }
    }

    override fun toString(): String {
        return "[Group \"$path\" routes=${routes.size} ${routes.map(Route::toString).joinToString(" ")}]"
    }
}


class Router(init: Group.() -> Unit) {
    val rootGroup = Group("/")
    init { init(rootGroup) }
    fun handler(): Handler = rootGroup.handler()
    operator fun invoke(request: Request): Response = handler()(request)
}
