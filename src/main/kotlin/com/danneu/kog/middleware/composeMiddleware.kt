package com.danneu.kog.middleware

import com.danneu.kog.Middleware

/** Compose middleware functions into a single middleware function.
 *
 *  Example:
 *
 *      composeMiddleware(a, b, c) becomes (handler) -> a(b(c(handler)))
 *
 *  `a` will touch the request first as it's coming in, and the response last as it's going out.
 *
 *  Arguments may be null to make it easier to conditionally add them, they'll just get filtered out:
 *
 *      composeMiddleware(
 *          serveStatic("public"),
 *          if (KOG_ENV == Development) logger() else null
 *      )
 */
fun composeMiddleware(wares: Collection<Middleware?>): Middleware {
    return wares
        .filterNotNull()
        .fold(identity, { final, next -> { handler -> final(next(handler)) } })
}

fun composeMiddleware(vararg wares: Middleware?): Middleware {
    return composeMiddleware(wares.asList())
}
