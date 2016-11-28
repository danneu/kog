package com.danneu.kog

import com.google.common.base.CaseFormat
import kotlin.reflect.KClass
import kotlin.reflect.primaryConstructor

typealias HeaderPair = Pair<Header, String>

// FIXME: Not sure how to create an interface with default implementations that always returning the implementing self.
// For now I use these unchecked `this as T` returns.
interface HasHeaders <out T> {
    val headers: MutableList<HeaderPair>

    fun getHeader(key: Header): String? {
        return headers.find { it.first == key }?.second
    }

    // if value is null, then the header does not get set
    fun setHeader(key: Header, value: String?): T {
        if (value == null) return this as T
        removeHeader(key)
        appendHeader(key, value)
        return this as T
    }

    // if value is null, then the header does not get set
    fun appendHeader(key: Header, value: String?): T {
        if (value == null) return this as T
        headers.add(key to value)
        return this as T
    }

    fun removeHeader(key: Header): T {
        headers.removeIf { it.first == key }
        return this as T
    }
}


sealed class Header {
    //fun equals(other: Header):Boolean = true

    class Custom(key: String) : Header() {
        val key = key.toCookieCase()
        override fun equals(other: Any?): Boolean = when (other) {
            is Custom -> key == other.key
            else -> false
        }
    }

    // AUTHENTICATION

    object WwwAuthenticate : Header()
    object Authorization : Header()
    object ProxyAuthenticate : Header()
    object ProxyAuthorization : Header()

        // CACHING

    object Age : Header()
    object CacheControl : Header()
    object Expires : Header()
    object Pragma : Header()
    object Warning : Header()

        // CLIENT HINTS

    object AcceptCh : Header()
    object ContentDpr : Header()
    object Dpr : Header()
    object Downlink : Header()
    object SaveData : Header()
    object ViewportWidth : Header()
    object Width : Header()

        // CONDITIONALS

    object LastModified : Header()
    object Etag : Header()
    object IfMatch : Header()
    object IfNoneMatch : Header()
    object IfModifiedSince : Header()
    object IfUnmodifiedSince : Header()

        // CONNECTION MANAGEMENT

    object Connection : Header()
    object KeepAlive : Header()

        // CONTENT NEGOTIATION

    object Accept : Header()
    object AcceptCharset : Header()
    object AcceptEncoding : Header()
    object AcceptLanguage : Header()

        // CONTROLS

    object Expect : Header()
    object MaxForwards : Header()

        // COOKIES

    object Cookie : Header()
    object SetCookie : Header()
    object Cookie2 : Header()
    object SetCookie2 : Header()

        // CORS

    object AccessControlAllowOrigin : Header()
    object AccessControlAllowCredentials : Header()
    object AccessControlAllowHeaders : Header()
    object AccessControlAllowMethods : Header()
    object AccessControlExposeHeaders : Header()
    object AccessControlMaxAge : Header()
    object AccessControlRequestHeaders : Header()
    object AccessControlRequestMethod : Header()
    object Origin : Header()

        // DO NOT TRACK

    object Dnt : Header()
    object Tk : Header()

        // DOWNLOADS

    object ContentDisposition : Header()

        // MESSAGE BODY INFO

    object ContentLength : Header()
    object ContentType : Header()
    object ContentEncoding : Header()
    object ContentLanguage : Header()
    object ContentLocation : Header()

        // MESSAGE ROUTING

    object Via : Header()

        // REDIRECTS

    object Location : Header()

        // REQUEST CONTEXT

    object From : Header()
    object Host : Header()
    object Referer : Header() // sic
    object ReferrerPolicy : Header()
    object UserAgent : Header()

        // RESPONSE CONTEXT

    object Allow : Header()
    object Server : Header()

        // RANGE REQUESTS

    object AcceptRanges : Header()
    object Range : Header()
    object IfRange : Header()
    object ContentRange : Header()

        // SECURITY

    object ContentSecurityPolicy : Header()
    object ContentSecurityPolicyReportOnly : Header()
    object PublicKeyPins : Header()
    object PublicKeyPinsReportOnly : Header()
    object StrictTransportSecurity : Header()
    object UpgradeInsecureRequests : Header()
    object XContentTypeOptions : Header()
    object XFrameOptions : Header()
    object XXssProtection : Header()

        // SERVER-SENT EVENTS

    object PingFrom : Header()
    object PingTo : Header()
    object LastEventId : Header()

        // TRANSFER ENCODING

    object TransferEncoding : Header()
    object Te : Header()
    object Trailer : Header()

        // WEBSOCKETS

    object SecWebSocketKey : Header()
    object SecWebSocketExtensions : Header()
    object SecWebSocketAccept : Header()
    object SecWebSocketProtocol : Header()
    object SecWebSocketVersion : Header()

        // OTHER

    object Date : Header()
    object Link : Header()
    object RetryAfter : Header()
    object Upgrade : Header()
    object Vary : Header()
    object XContentDuration : Header()
    object XDnsPrefetchControl : Header()
    object XRequestedWith : Header()
    object XUaCompatible : Header()

    // Hyphenates capitalize-letter boundaries.
    //
    // Example: "XForwardedFor" -> "X-Forwarded-For"
    override fun toString(): String {
        return when (this) {
            is Custom -> this.key.toCookieCase()
            else ->  CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_HYPHEN, javaClass.kotlin.simpleName).toCookieCase()
        }
    }

    companion object {
        fun fromString(string: String): Header {
            val className = CaseFormat.LOWER_HYPHEN.to(CaseFormat.UPPER_CAMEL, string.toLowerCase())
            val qualifiedName = "com.danneu.kog.Header$$className"
            val kclass = try { Class.forName(qualifiedName).kotlin } catch (e: ClassNotFoundException) { null }
            return if (kclass == null) { Header.Custom(string) } else { kclass.objectInstance as Header }
        }
    }
}



// "foo-bar-qux" -> "Foo-Bar-Qux"
fun String.toCookieCase(): String {
    return this
        .replace(Regex("""(?<=\b)([a-z])"""), { it.value.toUpperCase() })
        .replace(Regex("""(?<=\b[A-Z])([^-]+)"""), { it.value.toLowerCase() })
}
