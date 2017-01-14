package com.danneu.kog

import com.danneu.kog.negotiation.Encoding


class Negotiator(val request: Request) {

    // ACCEPT-ENCODING NEGOTIATION

    /** List of encodings send by client
     */
    val encodings: List<Encoding> = run {
        val header = request.getHeader(Header.AcceptEncoding) ?: return@run listOf(Encoding("identity"))
        Encoding.parse(header).let { Encoding.prioritize(it) }
    }

    /** Returns the client's most-preferred encoding
     */
    fun acceptableEncoding(availables: List<String>): String? {
        return encodings.find { encoding ->
            availables.any { avail -> encoding.acceptable(avail) }
        }?.name
    }
}
