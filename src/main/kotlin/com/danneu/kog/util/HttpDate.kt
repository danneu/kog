package com.danneu.kog.util

import org.joda.time.format.DateTimeFormat
import java.util.Locale
import org.joda.time.DateTime


// Implements de/serializations between joda DateTime<->String for dates
// in http headers like Last-Modified and cookie Expires.
//
// Example HTTP date: "Wed, 15 Nov 1995 04:58:08 GMT"
//
// HTTP date spec: https://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.3.1


object HttpDate {
    fun toString(date: DateTime): String {
        return date.toString(HttpDateFormatter)
    }

    fun fromString(string: String): DateTime? {
        return DateTime.parse(string, HttpDateFormatter)
    }

    // rfc2616 explains rfc1123
    // NOTE: java has java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME
    private val HttpDateFormatter = DateTimeFormat
        .forPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'")
        .withZoneUTC()
        .withLocale(Locale.US)
}
