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
        private const val GET = "GET"
        private const val HEAD = "HEAD"
        private const val POST = "POST"
        private const val PUT = "PUT"
        private const val DELETE = "DELETE"
        private const val OPTIONS = "OPTIONS"
        private const val TRACE = "TRACE"
        private const val PATCH = "PATCH"

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
