package com.danneu.kog

typealias HeaderPair = Pair<Header, String>

// Implements default header-related methods for Request/Response.
//
// Initially implemented this as an interface, but did not
// like how the toType() hack could not be made internal since
// I don't want the hack to be exposed on Request/Response
// public API.
abstract class HasHeaders<out T> {
    abstract val headers: MutableList<HeaderPair>

    // Allows us to avoid unchecked cast
    internal abstract fun toType(): T

    fun getHeader(key: Header): String? {
        return headers.find { it.first == key }?.second
    }

    // if value is null, then the header does not get set
    fun setHeader(key: Header, value: String?): T {
        if (value == null) return this.toType()
        removeHeader(key)
        appendHeader(key, value)
        return this.toType()
    }

    // if value is null, then the header does not get set
    fun appendHeader(key: Header, value: String?): T {
        if (value == null) return this.toType()
        headers.add(key to value)
        return this.toType()
    }

    fun removeHeader(key: Header): T {
        headers.removeIf { it.first == key }
        return this.toType()
    }
}

interface HasContentType {
    var contentType: ContentType?
}

