package com.danneu.kog.batteries.basicAuth


import com.danneu.kog.Header
import com.danneu.kog.Request
import com.danneu.kog.Response
import com.danneu.kog.utf8
import java.util.Base64


internal typealias BasicAuthCreds = Pair<String, String>


// RFC2045-MIME variant of Base64
private fun decodeMime64(string: String): String? = try {
    Base64.getMimeDecoder().decode(string).utf8()
} catch(_: IllegalArgumentException) {
    null
}


fun parseCreds(req: Request): BasicAuthCreds? {
    val authz = req.getHeader(Header.Authorization) ?: return null
    if (authz.take("Basic ".length) != "Basic ") return null
    val creds64 = authz.drop("Basic ".length)
    val creds = decodeMime64(creds64)?.split(":", limit = 2) ?: return null
    if (creds.size != 2) return null
    return creds[0] to creds[1]
}


fun challengeResponse(): Response {
    return Response.unauthorized()
        .setHeader(Header.WwwAuthenticate, "Basic realm=\"User Visible Realm\"")
}
