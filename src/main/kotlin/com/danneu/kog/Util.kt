package com.danneu.kog

import java.net.URLDecoder
import java.net.URLEncoder
import java.security.MessageDigest
import java.time.Duration


// Decodes www-form-urlencoded. Doesn't do any nesting for now.
fun formDecode(encoded: String?): Map<String, String> {
    if (encoded == null) { return emptyMap() }
    return encoded.split("&", limit = 100)
        .map { it.split("=", limit = 2) }
        .map { list -> if (list.size == 2) { list[0] to list[1] } else { null }}
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


// decimal digit to hex nibble lookup
private val nibble = arrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f')

fun Byte.toHexString(): String {
    val digit = this.toInt()
    val nib1 = nibble[digit shr 4 and 0x0f]
    val nib2 = nibble[digit and 0x0f]
    return "$nib1$nib2"
}

fun ByteArray.toHexString(): String {
    return StringBuilder().apply {
        for (b in this@toHexString) {
            append(b.toHexString())
        }
    }.toString()
}

fun Int.toHexString(): String {
    return Integer.toHexString(this)
}

fun Long.toHexString(): String {
    return java.lang.Long.toHexString(this)
}

fun ByteArray.utf8(): String {
    return this.toString(Charsets.UTF_8)
}

fun ByteArray.md5(): ByteArray {
    return MessageDigest.getInstance("MD5").digest(this)
}

fun ByteArray.sha1(): ByteArray {
    return MessageDigest.getInstance("SHA1").digest(this)
}

// Ensure a duration does not exceed a min and max length
fun Duration.clamp(min: Duration, max: Duration) = when {
    this < min -> min
    this > max -> max
    else -> this
}
