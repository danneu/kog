
package com.danneu.kog

enum class Status(val code: Int, val phrase: String, val redirect: Boolean = false, val empty: Boolean = false, val retry: Boolean = false) {
    // 1xx
    `continue`(100, "Continue"),
    switchingProtocols(101, "Switching Protocols"),
    processing(102, "Processing"),
    // 2xx
    ok(200, "OK"),
    created(201, "Created"),
    accepted(202, "Accepted"),
    nonAuthoritativeInformation(203, "Non-Authoritative Information"),
    noContent(204, "No Content", empty = true),
    resetContent(205, "Reset Content", empty = true),
    partialContent(206, "Partial Content"),
    multiStatus(207, "Multi Status"),
    alreadyReported(208, "Already Reported"),
    imUsed(226, "IM Used"),
    // 3xx
    multipleChoices(300, "Multiple Choices", redirect = true),
    movedPermanently(301, "Moved Permanently", redirect = true),
    found(302, "Found", redirect = true),
    seeOther(303, "See Other"),
    notModified(304, "Not Modified", empty = true),
    useProxy(305, "Use Proxy"),
    reserved(306, "Reserved"),
    temporaryRedirect(307, "Temporary Redirect"),
    permanentRedirect(308, "Permanent Redirect"),
    // 4xx
    badRequest(400, "Bad Request"),
    unauthorized(401, "Unauthorized"),
    paymentRequired(402, "Payment Required"),
    forbidden(403, "Forbidden"),
    notFound(404, "Not Found"),
    methodNotAllowed(405, "Method Not Allowed"),
    notAcceptable(406, "Not Acceptable"),
    proxyAuthenticationRequired(407, "Proxy Authentication Required"),
    requestTimeout(408, "Request Timeout"),
    conflict(409, "Conflict"),
    gone(410, "Gone"),
    lengthRequired(411, "Length Required"),
    // TODO: Finish
    // 5xx
    internalError(500, "Internal Error"),
    notImplemented(501, "Not Implemented"),
    badGateway(502, "Bad Gateway", retry = true),
    serviceUnavailable(503, "Service Unavailable", retry = true),
    gatewayTimeout(504, "Gateway Timeout", retry = true),
    // xxx
    unknown(0, "Unknown")
}
