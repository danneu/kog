
package com.danneu.kog


enum class Status(val code: Int, val redirect: Boolean = false, val empty: Boolean = false, val retry: Boolean = false) {
    // 1xx INFORMATIONAL

    Continue(100),
    SwitchingProtocols(101),
    Processing(102),

    // 2xx SUCCESS

    Ok(200),
    Created(201),
    Accepted(202),
    NonAuthoritativeInformation(203),
    NoContent(204, empty = true),
    ResetContent(205, empty = true),
    PartialContent(206),
    MultiStatus(207),
    AlreadyReported(208),
    ImUsed(226),

    // 3xx REDIRECTION

    MultipleChoices(300, redirect = true),
    MovedPermanently(301, redirect = true),
    Found(302, redirect = true),
    SeeOther(303, redirect = true),
    NotModified(304, empty = true),
    UseProxy(305, redirect = true),
    Reserved(306),
    TemporaryRedirect(307, redirect = true),
    PermanentRedirect(308, redirect = true),

    // 4xx CLIENT ERRORS

    BadRequest(400),
    Unauthorized(401),
    PaymentRequired(402),
    Forbidden(403),
    NotFound(404),
    MethodNotAllowed(405),
    NotAcceptable(406),
    ProxyAuthenticationRequired(407),
    RequestTimeout(408),
    Conflict(409),
    Gone(410),
    LengthRequired(411),
    PreconditionFailed(412),
    RequestEntityTooLarge(413),
    RequestURITooLong(414),
    UnsupportedMediaType(415),
    RequestedRangeNotSatisfiable(416),
    ExpectationFailed(417),
    ImATeapot(418),
    MisdirectedRequest(421),
    UnprocessableEntity(422),
    Locked(423),
    FailedDependency(424),
    UpgradeRequired(426),
    PreconditionRequired(428),
    TooManyRequests(429),
    RequestHeaderFieldsTooLarge(431),

    // 5xx SERVER ERRORS

    InternalServerError(500),
    NotImplemented(501),
    BadGateway(502, retry = true),
    ServiceUnavailable(503, retry = true),
    GatewayTimeout(504, retry = true),
    HttpVersionNotSupported(505),
    VariantAlsoNegotiates(506),
    InsufficientStorage(507),
    LoopDetected(508),
    NotExtended(510),
    NetworkAuthenticationRequired(511),

    // xxx

    Unknown(0);

    override fun toString() = name.toSpaceCamel()
}

private val spaceCamelRegex = Regex("""(?<=[a-zA-Z])([A-Z])""")

// "HelloWorldFoo" -> "Hello World Foo"
fun String.toSpaceCamel(): String {
    return this.replace(spaceCamelRegex, " $1")
}
