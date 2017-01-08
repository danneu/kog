package com.danneu.kog

import com.danneu.kog.middleware.identity

typealias Middleware = (Handler) -> Handler

fun composeMiddleware(vararg wares: Middleware): Middleware {
    return wares.fold(identity, { final, next -> { handler -> final(next(handler)) } })
}
