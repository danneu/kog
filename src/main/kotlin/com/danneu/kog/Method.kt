package com.danneu.kog

import java.util.Locale

enum class Method {
    // Common
    get,
    put,
    post,
    head,
    patch,
    delete,
    options,

    // Uncommon
    copy,
    lock,
    move,
    mkcol,
    merge,
    purge,
    trace,
    report,
    unlock,
    search,
    notify,
    connect,
    msearch,
    checkout,
    propfind,
    proppatch,
    subscribe,
    mkactivity,
    unsubscribe,

    // Internal
    unknown;

    companion object {
        fun fromString(value: String): Method {
            try {
                return valueOf(value.toLowerCase(Locale.ENGLISH))
            } catch (_: Exception) {
                return unknown
            }
        }
    }
}
