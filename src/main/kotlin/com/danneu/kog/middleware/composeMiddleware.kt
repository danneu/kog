package com.danneu.kog.middleware

import com.danneu.kog.Middleware

/** Compose middleware functions into a single middleware function.
 *
 *  Example:
 *
 *      composeMiddleware(a, b, c) becomes (handler) -> a(b(c(handler)))
 *
 *  `a` will touch the request first as it's coming in, and the response last as it's going out.
 */
fun composeMiddleware(vararg wares: Middleware): Middleware {
    return wares.fold(identity, { final, next -> { handler -> final(next(handler)) } })
}

fun composeMiddleware(wares: List<Middleware>): Middleware {
    return wares.fold(identity, { final, next -> { handler -> final(next(handler)) } })
}
