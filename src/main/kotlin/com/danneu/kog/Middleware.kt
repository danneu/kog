package com.danneu.kog

typealias Middleware = (Handler) -> Handler

fun composeMiddleware(vararg wares: Middleware): Middleware {
    val noop: Middleware = { handler -> handler }
    return wares.fold(noop, { final, next -> { handler -> final(next(handler)) } })
}