package com.danneu.kog.negotiation

import kotlin.comparisons.compareBy

// https://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html

// Note: "*/subtype" is invalid

// Note: If an Accept header field is present, and if the server cannot send a response which is acceptable
// according to the combined Accept field value, then the server SHOULD send a 406 (not acceptable) response.

data class MediaType(val type: String, val subtype: String, val q: Double = 1.0) {
    override fun toString(): String {
        return "MediaType(type='$type', subtype='$subtype', q=$q)"
    }

    fun acceptable(available: Pair<String, String>): Boolean {
        return when {
            // */* matches everything
            type == "*" && subtype == "*" ->
                true
            // text/* matches text/plain, text/html, text/foo, text/bar
            subtype == "*" ->
                type == available.first
            // text/plain only matches text/plain
            else ->
                type == available.first && subtype == available.second
        }
    }

    companion object {
        val regex = Regex("""^\s*([^\s\/;]+)\/([^;\s]+)\s*(?:;(.*))?$""")

        // TODO: Test malformed headers

        /** Parses a single segment pair
         */
        fun parse(string: String): MediaType? {
            val parts = regex.find(string)?.groupValues?.drop(1) ?: return null
            val type = parts[0]
            val subtype = parts[1]
            val q = QValue.parse(parts[2]) ?: 1.0
            return MediaType(type, subtype, q)
        }

        /** Parses comma-delimited string of types
         */
        fun parseHeader(header: String): List<MediaType> {
            return header.split(",").map(String::trim).mapNotNull { parse(it) }
        }

        /** Sorts media-types based on client priority
         */
        fun prioritize(xs: List<MediaType>): List<MediaType> {
            return xs.sortedWith(compareBy(
                { -it.q },
                { when {
                    it.type == "*" && it.subtype == "*" -> 1
                    it.subtype == "*" -> 0
                    else -> -1
                }}
            ))
        }
    }
}

