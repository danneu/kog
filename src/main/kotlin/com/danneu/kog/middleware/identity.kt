package com.danneu.kog.middleware

import com.danneu.kog.Middleware

/** Middleware that returns the handler that is passed to it.
 *
 * For some reason, the compiler sometimes complains about type ambiguity for an inlined `{ it }`, so that's
 * why this helper exists since it's annotated as Middleware. Though I'd prefer to figure out the cause of the
 * type issue and delete this redundancy.
 */
val identity: Middleware = { it }
