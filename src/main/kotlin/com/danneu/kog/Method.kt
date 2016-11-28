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
        fun fromString(value: String): Method = try {
            enumValueOf<Method>(value.toLowerCase().capitalize())
        } catch (_: IllegalArgumentException) {
            Unknown
        }
    }
}
