package com.danneu.kog


enum class Method {
    // COMMON

    Get,
    Head,
    Post,
    Put,
    Delete,
    Options,
    Trace,
    Patch,

    // INTERNAL

    Unknown;

    companion object {
        const val GET = "GET"
        const val HEAD = "HEAD"
        const val POST = "POST"
        const val PUT = "PUT"
        const val DELETE = "DELETE"
        const val OPTIONS = "OPTIONS"
        const val TRACE = "TRACE"
        const val PATCH = "PATCH"

        fun fromString(value: String) = when (value.toUpperCase()) {
            GET -> Get
            HEAD -> Head
            POST -> Post
            PUT -> Put
            DELETE -> Delete
            OPTIONS -> Options
            TRACE -> Trace
            PATCH -> Patch
            else -> Unknown
        }
    }
}
