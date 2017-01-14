package com.danneu.kog.cookies

import com.danneu.kog.percentEncode
import com.danneu.kog.util.HttpDate
import java.time.Duration
import java.time.OffsetDateTime

//
// Represents a response cookie
//

data class Cookie(
    val value: String,
    val path: String? = null,
    val domain: String? = null,
    val duration: Ttl = Ttl.Session,
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

    sealed class Ttl {
        object Session: Ttl() {
            override fun serialize(): String? = null
        }
        class Expires(val date: OffsetDateTime): Ttl() {
            override fun serialize(): String = "expires=${HttpDate.toString(date)}"
        }
        class MaxAge(val duration: Duration): Ttl() {
            override fun serialize(): String = "max-age=${duration.seconds}"
        }

        abstract fun serialize(): String?
    }
}
