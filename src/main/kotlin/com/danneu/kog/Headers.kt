package com.danneu.kog


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


sealed class Header(val key: String) {
    // Keys are canonicalized on creation. Nitpick: Maybe it should be done at another time.
    class Custom(key: String) : Header(key.toLowerCase()) {
        // Support Custom("foo") == Custom("FOO")
        override fun equals(other: Any?): Boolean = when (other) {
            is Custom -> key == other.key
            else -> false
        }
    }

    // AUTHENTICATION

    object WwwAuthenticate : Header(WWW_AUTHENTICATE)
    object Authorization : Header(AUTHORIZATION)
    object ProxyAuthenticate : Header(PROXY_AUTHENTICATE)
    object ProxyAuthorization : Header(PROXY_AUTHORIZATION)

    // CACHING

    object Age : Header(AGE)
    object CacheControl : Header(CACHE_CONTROL)
    object Expires : Header(EXPIRES)
    object Pragma : Header(PRAGMA)
    object Warning : Header(WARNING)

    // CLIENT HINTS

    object AcceptCh : Header(ACCEPT_CH)
    object ContentDpr : Header(CONTENT_DPR)
    object Dpr : Header(DPR)
    object Downlink : Header(DOWNLINK)
    object SaveData : Header(SAVE_DATA)
    object ViewportWidth : Header(VIEWPORT_WIDTH)
    object Width : Header(WIDTH)

    // CONDITIONALS

    object LastModified : Header(LAST_MODIFIED)
    object Etag : Header(ETAG)
    object IfMatch : Header(IF_MATCH)
    object IfNoneMatch : Header(IF_NONE_MATCH)
    object IfModifiedSince : Header(IF_MODIFIED_SINCE)
    object IfUnmodifiedSince : Header(IF_UNMODIFIED_SINCE)

    // CONNECTION MANAGEMENT

    object Connection : Header(CONNECTION)
    object KeepAlive : Header(KEEP_ALIVE)

    // CONTENT NEGOTIATION

    object Accept : Header(ACCEPT)
    object AcceptCharset : Header(ACCEPT_CHARSET)
    object AcceptEncoding : Header(ACCEPT_ENCODING)
    object AcceptLanguage : Header(ACCEPT_LANGUAGE)

    // CONTROLS

    object Expect : Header(EXPECT)
    object MaxForwards : Header(MAX_FORWARDS)

    // COOKIES

    object Cookie : Header(COOKIE)
    object SetCookie : Header(SET_COOKIE)
    object Cookie2 : Header(COOKIE2)
    object SetCookie2 : Header(SET_COOKIE2)

    // CORS

    object AccessControlAllowOrigin : Header(ACCESS_CONTROL_ALLOW_ORIGIN)
    object AccessControlAllowCredentials : Header(ACCESS_CONTROL_ALLOW_CREDENTIALS)
    object AccessControlAllowHeaders : Header(ACCESS_CONTROL_ALLOW_HEADERS)
    object AccessControlAllowMethods : Header(ACCESS_CONTROL_ALLOW_METHODS)
    object AccessControlExposeHeaders : Header(ACCESS_CONTROL_EXPOSE_HEADERS)
    object AccessControlMaxAge : Header(ACCESS_CONTROL_MAX_AGE)
    object AccessControlRequestHeaders : Header(ACCESS_CONTROL_REQUEST_HEADERS)
    object AccessControlRequestMethod : Header(ACCESS_CONTROL_REQUEST_METHOD)
    object Origin : Header(ORIGIN)

    // DO NOT TRACK

    object Dnt : Header(DNT)
    object Tk : Header(TK)

    // DOWNLOADS

    object ContentDisposition : Header(CONTENT_DISPOSITION)

    // MESSAGE BODY INFO

    object ContentLength : Header(CONTENT_LENGTH)
    object ContentType : Header(CONTENT_TYPE)
    object ContentEncoding : Header(CONTENT_ENCODING)
    object ContentLanguage : Header(CONTENT_LANGUAGE)
    object ContentLocation : Header(CONTENT_LOCATION)


    // MESSAGE ROUTING

    object Via : Header(VIA)

    // REDIRECTS

    object Location : Header(LOCATION)

    // REQUEST CONTEXT

    object From : Header(FROM)
    object Host : Header(HOST)
    object Referer : Header(REFERER) // sic
    object ReferrerPolicy : Header(REFERRER_POLICY)
    object UserAgent : Header(USER_AGENT)

    // RESPONSE CONTEXT

    object Allow : Header(ALLOW)
    object Server : Header(SERVER)

    // RANGE REQUESTS

    object AcceptRanges : Header(ACCEPT_RANGES)
    object Range : Header(RANGE)
    object IfRange : Header(IF_RANGE)
    object ContentRange : Header(CONTENT_RANGE)

    // SECURITY

    object ContentSecurityPolicy : Header(CONTENT_SECURITY_POLICY)
    object ContentSecurityPolicyReportOnly : Header(CONTENT_SECURITY_POLICY_REPORT_ONLY)
    object PublicKeyPins : Header(PUBLIC_KEY_PINS)
    object PublicKeyPinsReportOnly : Header(PUBLIC_KEY_PINS_REPORT_ONLY)
    object StrictTransportSecurity : Header(STRICT_TRANSPORT_SECURITY)
    object UpgradeInsecureRequests : Header(UPGRADE_INSECURE_REQUESTS)
    object XContentTypeOptions : Header(X_CONTENT_TYPE_OPTIONS)
    object XFrameOptions : Header(X_FRAME_OPTIONS)
    object XXssProtection : Header(X_XSS_PROTECTION)

    // SERVER-SENT EVENTS

    object PingFrom : Header(PING_FROM)
    object PingTo : Header(PING_TO)
    object LastEventId : Header(LAST_EVENT_ID)

    // TRANSFER ENCODING

    object TransferEncoding : Header(TRANSFER_ENCODING)
    object Te : Header(TE)
    object Trailer : Header(TRAILER)

    // WEBSOCKETS

    object SecWebSocketKey : Header(SEC_WEBSOCKET_KEY)
    object SecWebSocketExtensions : Header(SEC_WEBSOCKET_EXTENSIONS)
    object SecWebSocketAccept : Header(SEC_WEBSOCKET_ACCEPT)
    object SecWebSocketProtocol : Header(SEC_WEBSOCKET_PROTOCOL)
    object SecWebSocketVersion : Header(SEC_WEBSOCKET_VERSION)

    // OTHER

    object Date : Header(DATE)
    object Link : Header(LINK)
    object RetryAfter : Header(RETRY_AFTER)
    object Upgrade : Header(UPGRADE)
    object Vary : Header(VARY)
    object XContentDuration : Header(X_CONTENT_DURATION)
    object XDnsPrefetchControl : Header(X_DNS_PREFETCH_CONTROL)
    object XRequestedWith : Header(X_REQUESTED_WITH)
    object XUaCompatible : Header(X_UA_COMPATIBLE)

    // Hyphenates capitalize-letter boundaries.
    //
    // Example: "XForwardedFor" -> "X-Forwarded-For"
    override fun toString(): String = key

    companion object {
        private val ACCEPT = "accept"
        private val ACCEPT_CH = "accept-ch"
        private val ACCEPT_CHARSET = "accept-charset"
        private val ACCEPT_ENCODING = "accept-encoding"
        private val ACCEPT_LANGUAGE = "accept-language"
        private val ACCEPT_RANGES = "accept-ranges"
        private val ACCESS_CONTROL_ALLOW_CREDENTIALS = "access-control-allow-credentials"
        private val ACCESS_CONTROL_ALLOW_HEADERS = "access-control-allow-headers"
        private val ACCESS_CONTROL_ALLOW_METHODS = "access-control-allow-methods"
        private val ACCESS_CONTROL_ALLOW_ORIGIN = "access-control-allow-origin"
        private val ACCESS_CONTROL_EXPOSE_HEADERS = "access-control-expose-headers"
        private val ACCESS_CONTROL_MAX_AGE = "access-control-max-age"
        private val ACCESS_CONTROL_REQUEST_HEADERS = "access-control-request-headers"
        private val ACCESS_CONTROL_REQUEST_METHOD = "access-control-request-method"
        private val AGE = "age"
        private val ALLOW = "allow"
        private val AUTHORIZATION = "authorization"
        private val CACHE_CONTROL = "cache-control"
        private val CONNECTION = "connection"
        private val CONTENT_DISPOSITION = "content-disposition"
        private val CONTENT_DPR = "content-dpr"
        private val CONTENT_ENCODING = "content-encoding"
        private val CONTENT_LANGUAGE = "content-language"
        private val CONTENT_LENGTH = "content-length"
        private val CONTENT_LOCATION = "content-location"
        private val CONTENT_RANGE = "content-range"
        private val CONTENT_SECURITY_POLICY = "content-security-policy"
        private val CONTENT_SECURITY_POLICY_REPORT_ONLY = "content-security-policy-report-only"
        private val CONTENT_TYPE = "content-type"
        private val COOKIE = "cookie"
        private val COOKIE2 = "cookie2"
        private val DATE = "date"
        private val DNT = "dnt"
        private val DOWNLINK = "downlink"
        private val DPR = "dpr"
        private val ETAG = "etag"
        private val EXPECT = "expect"
        private val EXPIRES = "expires"
        private val FROM = "from"
        private val HOST = "host"
        private val IF_MATCH = "if-match"
        private val IF_MODIFIED_SINCE = "if-modified-since"
        private val IF_NONE_MATCH = "if-none-match"
        private val IF_RANGE = "if-range"
        private val IF_UNMODIFIED_SINCE = "if-unmodified-since"
        private val KEEP_ALIVE = "keep-alive"
        private val LAST_EVENT_ID = "last-event-id"
        private val LAST_MODIFIED = "last-modified"
        private val LINK = "link"
        private val LOCATION = "location"
        private val MAX_FORWARDS = "max-forwards"
        private val ORIGIN = "origin"
        private val PING_FROM = "ping-from"
        private val PING_TO = "ping-to"
        private val PRAGMA = "pragma"
        private val PROXY_AUTHENTICATE = "proxy-authenticate"
        private val PROXY_AUTHORIZATION = "proxy-authorization"
        private val PUBLIC_KEY_PINS = "public-key-pins"
        private val PUBLIC_KEY_PINS_REPORT_ONLY = "public-key-pins-report-only"
        private val RANGE = "range"
        private val REFERER = "referer"
        private val REFERRER_POLICY = "referrer-policy"
        private val RETRY_AFTER = "retry-after"
        private val SAVE_DATA = "save-data"
        private val SEC_WEBSOCKET_ACCEPT = "sec-websocket-accept"
        private val SEC_WEBSOCKET_EXTENSIONS = "sec-websocket-extensions"
        private val SEC_WEBSOCKET_KEY = "sec-websocket-key"
        private val SEC_WEBSOCKET_PROTOCOL = "sec-websocket-protocol"
        private val SEC_WEBSOCKET_VERSION = "sec-websocket-version"
        private val SERVER = "server"
        private val SET_COOKIE = "set-cookie"
        private val SET_COOKIE2 = "set-cookie2"
        private val STRICT_TRANSPORT_SECURITY = "strict-transport-security"
        private val TE = "te"
        private val TK = "tk"
        private val TRAILER = "trailer"
        private val TRANSFER_ENCODING = "transfer-encoding"
        private val UPGRADE = "upgrade"
        private val UPGRADE_INSECURE_REQUESTS = "upgrade-insecure-requests"
        private val USER_AGENT = "user-agent"
        private val VARY = "vary"
        private val VIA = "via"
        private val VIEWPORT_WIDTH = "viewport-width"
        private val WARNING = "warning"
        private val WIDTH = "width"
        private val WWW_AUTHENTICATE = "www-authenticate"
        private val X_CONTENT_DURATION = "x-content-duration"
        private val X_CONTENT_TYPE_OPTIONS = "x-content-type-options"
        private val X_DNS_PREFETCH_CONTROL = "x-dns-prefetch-control"
        private val X_FRAME_OPTIONS = "x-frame-options"
        private val X_REQUESTED_WITH = "x-requested-with"
        private val X_UA_COMPATIBLE = "x-ua-compatible"
        private val X_XSS_PROTECTION = "x-xss-protection"

        fun fromString(string: String): Header = when (string.toLowerCase()) {
            ACCEPT -> Accept
            ACCEPT_CH -> AcceptCh
            ACCEPT_CHARSET -> AcceptCharset
            ACCEPT_ENCODING -> AcceptEncoding
            ACCEPT_LANGUAGE -> AcceptLanguage
            ACCEPT_RANGES -> AcceptRanges
            ACCESS_CONTROL_ALLOW_CREDENTIALS -> AccessControlAllowCredentials
            ACCESS_CONTROL_ALLOW_HEADERS -> AccessControlAllowHeaders
            ACCESS_CONTROL_ALLOW_METHODS -> AccessControlAllowMethods
            ACCESS_CONTROL_ALLOW_ORIGIN -> AccessControlAllowOrigin
            ACCESS_CONTROL_EXPOSE_HEADERS -> AccessControlExposeHeaders
            ACCESS_CONTROL_MAX_AGE -> AccessControlMaxAge
            ACCESS_CONTROL_REQUEST_HEADERS -> AccessControlRequestHeaders
            ACCESS_CONTROL_REQUEST_METHOD -> AccessControlRequestMethod
            AGE -> Age
            ALLOW -> Allow
            AUTHORIZATION -> Authorization
            CACHE_CONTROL -> CacheControl
            CONNECTION -> Connection
            CONTENT_DISPOSITION -> ContentDisposition
            CONTENT_DPR -> ContentDpr
            CONTENT_ENCODING -> ContentEncoding
            CONTENT_LANGUAGE -> ContentLanguage
            CONTENT_LENGTH -> ContentLength
            CONTENT_LOCATION -> ContentLocation
            CONTENT_RANGE -> ContentRange
            CONTENT_SECURITY_POLICY -> ContentSecurityPolicy
            CONTENT_SECURITY_POLICY_REPORT_ONLY -> ContentSecurityPolicyReportOnly
            CONTENT_TYPE -> ContentType
            COOKIE -> Cookie
            COOKIE2 -> Cookie2
            DATE -> Date
            DNT -> Dnt
            DOWNLINK -> Downlink
            DPR -> Dpr
            ETAG -> Etag
            EXPECT -> Expect
            EXPIRES -> Expires
            FROM -> From
            HOST -> Host
            IF_MATCH -> IfMatch
            IF_MODIFIED_SINCE -> IfModifiedSince
            IF_NONE_MATCH -> IfNoneMatch
            IF_RANGE -> IfRange
            IF_UNMODIFIED_SINCE -> IfUnmodifiedSince
            KEEP_ALIVE -> KeepAlive
            LAST_EVENT_ID -> LastEventId
            LAST_MODIFIED -> LastModified
            LINK -> Link
            LOCATION -> Location
            MAX_FORWARDS -> MaxForwards
            ORIGIN -> Origin
            PING_FROM -> PingFrom
            PING_TO -> PingTo
            PRAGMA -> Pragma
            PROXY_AUTHENTICATE -> ProxyAuthenticate
            PROXY_AUTHORIZATION -> ProxyAuthorization
            PUBLIC_KEY_PINS -> PublicKeyPins
            PUBLIC_KEY_PINS_REPORT_ONLY -> PublicKeyPinsReportOnly
            RANGE -> Range
            REFERER -> Referer
            REFERRER_POLICY -> ReferrerPolicy
            RETRY_AFTER -> RetryAfter
            SAVE_DATA -> SaveData
            SEC_WEBSOCKET_ACCEPT -> SecWebSocketAccept
            SEC_WEBSOCKET_EXTENSIONS -> SecWebSocketExtensions
            SEC_WEBSOCKET_KEY -> SecWebSocketKey
            SEC_WEBSOCKET_PROTOCOL -> SecWebSocketProtocol
            SEC_WEBSOCKET_VERSION -> SecWebSocketVersion
            SERVER -> Server
            SET_COOKIE -> SetCookie
            SET_COOKIE2 -> SetCookie2
            STRICT_TRANSPORT_SECURITY -> StrictTransportSecurity
            TE -> Te
            TK -> Tk
            TRAILER -> Trailer
            TRANSFER_ENCODING -> TransferEncoding
            UPGRADE -> Upgrade
            UPGRADE_INSECURE_REQUESTS -> UpgradeInsecureRequests
            USER_AGENT -> UserAgent
            VARY -> Vary
            VIA -> Via
            VIEWPORT_WIDTH -> ViewportWidth
            WARNING -> Warning
            WIDTH -> Width
            WWW_AUTHENTICATE -> WwwAuthenticate
            X_CONTENT_DURATION -> XContentDuration
            X_CONTENT_TYPE_OPTIONS -> XContentTypeOptions
            X_DNS_PREFETCH_CONTROL -> XDnsPrefetchControl
            X_FRAME_OPTIONS -> XFrameOptions
            X_REQUESTED_WITH -> XRequestedWith
            X_UA_COMPATIBLE -> XUaCompatible
            X_XSS_PROTECTION -> XXssProtection
            else -> Custom(string.toLowerCase())
        }
    }

}



// "foo-bar-qux" -> "Foo-Bar-Qux"
fun String.toCookieCase(): String {
    return this.toLowerCase()
        // Capitalize the first letter after each boundary
        .replace(Regex("""(?<=\b)([a-z])"""), { it.value.toUpperCase() })
}
