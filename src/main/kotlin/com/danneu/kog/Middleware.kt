package com.danneu.kog

typealias Middleware = (Handler) -> Handler

// Used as a pass-through noop
val identity: Middleware = { handler -> handler }

fun composeMiddleware(vararg wares: Middleware): Middleware {
    return wares.fold(identity, { final, next -> { handler -> final(next(handler)) } })
}
