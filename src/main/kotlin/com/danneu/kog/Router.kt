package com.danneu.kog

import java.util.Stack


// HELPERS


private fun toPath(vararg paths: String): String {
    return "/" + paths.flatMap { it.split("/") }.filter(String::isNotEmpty).joinToString("/")
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


class Group(val path: String, init: Group.() -> Unit = {}) {
    val routes = Stack<Route>()
    val stack = Stack<StackItem>()
    val wares = Stack<Middleware>()
    init { init() }

    fun method(method: Method): (String, Handler) -> Unit = { path: String, handler: Handler ->
        val route = Route(method, toPath(this.path, path), handler)
        routes.push(route)
        stack.push(StackItem.Route(route))
        wares.push(route.middleware())
    }

    fun get(path: String, handler: Handler) = method(Method.get)(path, handler)
    fun put(path: String, handler: Handler) = method(Method.put)(path, handler)
    fun post(path: String, handler: Handler) = method(Method.post)(path, handler)
    fun head(path: String, handler: Handler) = method(Method.head)(path, handler)
    fun patch(path: String, handler: Handler) = method(Method.patch)(path, handler)
    fun delete(path: String, handler: Handler) = method(Method.delete)(path, handler)
    fun options(path: String, handler: Handler) = method(Method.options)(path, handler)

    fun group(path: String, subinit: Group.() -> Unit): Group {
        val group = Group(toPath(this.path, path), subinit)
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
