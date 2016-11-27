package com.danneu.kog.cookies

import com.danneu.kog.percentEncode
import com.danneu.kog.util.HttpDate
import org.joda.time.DateTime


//
// Represents a response cookie
//


sealed class Duration {
    object Session: Duration() {
        override fun serialize(): String? = null
    }
    class Expires(val date: DateTime): Duration() {
        override fun serialize(): String = "expires=${HttpDate.toString(date)}"
    }
    class MaxAge(val seconds: Int): Duration() {
        override fun serialize(): String = "max-age=$seconds"
    }

    abstract fun serialize(): String?
}


data class Cookie(
    val value: String,
    val path: String? = null,
    val domain: String? = null,
    val duration: Duration = Duration.Session,
    val secure: Boolean? = null,
    val httpOnly: Boolean? = null,
    val firstPartyOnly: Boolean? = null
) {
    fun serialize(name: String): String = mutableListOf("$name=${percentEncode(value)}").apply {
        if (path != null) add("path=$path")
        if (domain != null) add("domain=$domain")
        duration.serialize()?.let { add(it) }
        if (httpOnly == true) add("HttpOnly")
        if (secure == true) add("Secure")
        if (firstPartyOnly == true) add("First-Party-Only")
    }.joinToString("; ")
}
