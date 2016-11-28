package com.danneu.kog.batteries

import com.danneu.kog.Middleware
import com.danneu.kog.batteries.basicAuth.challengeResponse
import com.danneu.kog.batteries.basicAuth.parseCreds


fun basicAuth(isAuthenticated: (String, String) -> Boolean): Middleware = { handler -> handler@ { request ->
    val (name, pass) = parseCreds(request) ?: return@handler challengeResponse()
    if (!isAuthenticated(name, pass)) return@handler challengeResponse()
    handler(request)
}}

