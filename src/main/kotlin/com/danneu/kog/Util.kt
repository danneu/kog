package com.danneu.kog

import java.net.URLDecoder


// Decodes www-form-urlencoded. Doesn't do any nesting for now.
fun formDecode(encoded: String?): Map<String, String> {
    if (encoded == null) { return emptyMap() }
    return encoded.split("&")
        .map { it.split("=", limit = 2) }
        .map { list -> if (list.size == 2) { Pair(list[0], list[1]) } else { null }}
        .filterNotNull()
        .toMap()
}


fun urlDecode(str: String, encoding: String = "UTF-8"): String {
    return URLDecoder.decode(str, encoding)
}

fun <K, V> Map<K, V>.mutableCopy(): MutableMap<K, V> = java.util.HashMap(this)


// Apply a function to a value if the value is not null
fun <A, B> notNullThen(value: A?, xform: (A) -> B): B? {
    if (value == null) { return null }
    return xform(value)
}