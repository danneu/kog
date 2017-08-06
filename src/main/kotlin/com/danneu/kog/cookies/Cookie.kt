package com.danneu.kog.cookies

import com.danneu.kog.util.Util
import com.danneu.kog.util.HttpDate
import java.time.Duration
import java.time.OffsetDateTime

class SerializeException(message: String): Exception(message)

/**
 * RegExp to match field-content in RFC 7230 sec 3.2
 *
 * field-content = field-vchar [ 1*( SP / HTAB ) field-vchar ]
 * field-vchar   = VCHAR / obs-text
 * obs-text      = %x80-FF
 */
val fieldContentRegExp = Regex("""^[\u0009\u0020-\u007e\u0080-\u00ff]+$""")

//
// Represents a response cookie
//

data class Cookie(
    val value: String,
    val path: String? = null,
    val domain: String? = null,
    val duration: Ttl = Ttl.Session,
    val secure: Boolean = false,
    val httpOnly: Boolean = false,
    val firstPartyOnly: Boolean = false,
    val sameSite: SameSite? = null
) {
    fun serialize(key: String): String {
        if (!fieldContentRegExp.matches(key)) {
            throw SerializeException("Cookie key \"$key\" is invalid")
        }

        val encodedValue = Util.percentEncode(this.value)

        if (encodedValue.isNotEmpty() && !fieldContentRegExp.matches(encodedValue)) {
            throw SerializeException("Cookie encoded value \"$encodedValue\" is invalid")
        }

        return StringBuilder("$key=$encodedValue").also { builder ->
            this.duration.serialize()?.let { serialized ->
                builder.append("; $serialized")
            }

            this.domain?.let { domain ->
                if (!fieldContentRegExp.matches(domain)) {
                    throw SerializeException("Cookie domain \"$domain\" is invalid")
                }
                builder.append("; Domain=$domain")
            }

            this.path?.let { path ->
                if (!fieldContentRegExp.matches(path)) {
                    throw SerializeException("Cookie path \"$path\" is invalid")
                }
                builder.append("; Path=$path")
            }

            if (this.httpOnly) {
                builder.append("; HttpOnly")
            }

            if (this.secure) {
                builder.append("; Secure")
            }

            if (this.firstPartyOnly) {
                builder.append("; First-Party-Only")
            }

            this.sameSite?.serialize()?.let { serialized ->
                builder.append("; $serialized")
            }
        }.toString()
    }
}

enum class SameSite {
    Strict {
        override fun serialize() = "SameSite=Strict"
    },
    Lax {
        override fun serialize() = "SameSite=Lax"
    };

    abstract fun serialize(): String
}

sealed class Ttl {
    object Session: Ttl() {
        override fun serialize(): String? = null
    }
    class Expires(val date: OffsetDateTime): Ttl() {
        override fun serialize(): String = "Expires=${HttpDate.toString(date)}"
    }
    class MaxAge(val duration: Duration): Ttl() {
        override fun serialize(): String = "Max-Age=${duration.seconds}"
    }

    abstract fun serialize(): String?
}
