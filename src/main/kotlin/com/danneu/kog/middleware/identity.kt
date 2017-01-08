package com.danneu.kog.middleware

import com.danneu.kog.Middleware

/** Middleware that returns the handler that is passed to it.
 */
val identity: Middleware = { handler -> handler }
