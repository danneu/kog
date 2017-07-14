package com.danneu.kog

/**
 * The possible values for a request's protocol.
 */
enum class Protocol {
    Http_1_0,
    Http_1_1,
    Http_2;

    companion object {
        // Jetty always gives us uppercase.
        fun fromString(string: String) = when (string) {
            "HTTP/1.0" ->
                Http_1_0
            "HTTP/1.1" ->
                Http_1_1
            "HTTP/2" ->
                Http_2
            else ->
                Http_1_1
        }
    }
}


