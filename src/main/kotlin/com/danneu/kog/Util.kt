package com.danneu.kog

import java.net.URLDecoder
import java.net.URLEncoder


// Decodes www-form-urlencoded. Doesn't do any nesting for now.
fun formDecode(encoded: String?): Map<String, String> {
    if (encoded == null) { return emptyMap() }
    return encoded.split("&")
        .map { it.split("=", limit = 2) }
        .map { list -> if (list.size == 2) { Pair(list[0], list[1]) } else { null }}
        .filterNotNull()
        .toMap()
}


fun urlDecode(string: String, encoding: String = "utf-8"): String {
    return URLDecoder.decode(string, encoding)
}

// Note: space becomes "+"
fun urlEncode(string: String): String {
    return URLEncoder.encode(string, "utf-8")
}

// Note: space becomes "%20"
fun percentEncode(string: String): String {
    return URLEncoder.encode(string, "utf-8").replace("+", "%20")
}

fun percentDecode(string: String): String {
    return URLDecoder.decode(string, "utf-8")
}


fun <K, V> Map<K, V>.mutableCopy(): MutableMap<K, V> = java.util.HashMap(this)





