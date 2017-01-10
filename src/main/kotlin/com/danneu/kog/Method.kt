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
        val GET = "GET"
        val HEAD = "HEAD"
        val POST = "POST"
        val PUT = "PUT"
        val DELETE = "DELETE"
        val OPTIONS = "OPTIONS"
        val TRACE = "TRACE"
        val PATCH = "PATCH"

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
