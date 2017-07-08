package com.danneu.kog.middleware

import com.danneu.kog.Middleware

/** Compose middleware functions into a single middleware function.
 *
 *  Example:
 *
 *      compose(a, b, c) becomes (handler) -> a(b(c(handler)))
 *
 *  `a` will touch the request first as it's coming in, and the response last as it's going out.
 *
 *  Arguments may be null to make it easier to conditionally add them, they'll just get filtered out:
 *
 *      compose(
 *          serveStatic("public"),
 *          if (KOG_ENV == Development) logger() else null
 *      )
 */
fun compose(wares: Iterable<Middleware?>): Middleware {
    return wares
        .filterNotNull()
        .fold({ ware -> ware }, { final, next -> { handler -> final(next(handler)) } })
}

fun compose(vararg wares: Middleware?): Middleware {
    return compose(wares.asIterable())
}
