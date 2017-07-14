package com.danneu.kog

/**
 * The possible values for a request's protocol.
 */
enum class Protocol {
    HTTP_1_0,
    HTTP_1_1,
    HTTP_2;

    companion object {
        // Jetty always gives us uppercase.
        fun fromString(string: String) = when (string) {
            "HTTP/1.0" -> HTTP_1_0
            "HTTP/1.1" -> HTTP_1_1
            "HTTP/2" -> HTTP_2
            else -> HTTP_1_1
        }
    }
}


