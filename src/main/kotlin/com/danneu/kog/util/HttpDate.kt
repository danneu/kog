package com.danneu.kog.util


import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter


// Implements de/serializations between DateTime<->String for dates
// in http headers like Last-Modified and cookie Expires.
//
// Example HTTP date: "Wed, 15 Nov 1995 04:58:08 GMT"
//
// HTTP date spec: https://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.3.1


object HttpDate {
    fun toString(date: OffsetDateTime): String {
        return date.format(HttpDateFormatter)
    }

    fun fromString(string: String): OffsetDateTime? {
        return OffsetDateTime.parse(string, HttpDateFormatter)
    }

    // rfc2616 explains rfc1123
    private val HttpDateFormatter = DateTimeFormatter.RFC_1123_DATE_TIME
}
