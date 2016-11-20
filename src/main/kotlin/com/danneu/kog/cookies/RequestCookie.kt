package com.danneu.kog.cookies

import com.danneu.kog.urlDecode
import org.joda.time.DateTime
import java.net.URLDecoder


//fun cookies(): Middleware = { handler -> { req ->
//    val response = handler(cookieRequest(handler))
//    return cookieResponse(response)
//}}


//fun cookieRequest(request: Request): Request {
//    val header = request.getHeader("cookie")
//    if (header != null) {
//        request.cookies = parse(header)
//    } else {
//        request.cookies = emptyMap()
//    }
//    return request
//}


// PARSING REQUEST


fun parse(header: String?): Map<String, String> {
    if (header == null) { return emptyMap() }
    val pairs = header.split(";").map(String::trim).filter(String::isNotEmpty).map(::parsePair)
    return pairs.filterNotNull().filter { (k, v) -> v.isNotEmpty() }.toMap()
}


private fun parsePair(str: String): Pair<String, String>? {
    val pair = str.split("=", limit = 2)
    if (pair.size != 2) { return null }
    return Pair(pair[0].trim(), urlDecode(unwrapQuotes(pair[1].trim())))
}


private fun unwrapQuotes(str: String): String {

    val re = Regex("""(^"|"$)""")
    return str.replace(re, "")
}




//class ResponseCookie(var value: String, var domain: String, var path: String = "/", var secure: Boolean = false, var httpOnly: Boolean = true, var maxAge: Long, var expires: DateTime) {
//}

