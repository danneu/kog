package com.danneu.kog

import com.danneu.kog.negotiation.Encoding
import com.danneu.kog.negotiation.MediaType

// The negotiator takes a request and determines the client's best-fitting preference from a list of available options
// by querying the request's Accept and Accept-* headers.

class Negotiator(val request: Request) {

    // ACCEPT-ENCODING NEGOTIATION

    /** List of the preferred encodings send by client
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

    // ACCEPT NEGOTIATION

    /** List of preferred mediaTypes sent by the client
     */
    val mediaTypes: List<MediaType> = run {
        val header = request.getHeader(Header.Accept) ?: return@run emptyList()
        MediaType.parseHeader(header).distinct().let { MediaType.prioritize(it) }
    }

    /** Returns our available media type that the user most prefers
     */
    fun acceptableMediaType(availables: Collection<Pair<String, String>>): Pair<String, String>? {
        if (mediaTypes.isEmpty()) return availables.firstOrNull()
        return availables.find { avail ->
            mediaTypes.any { type -> type.acceptable(avail) }
        }
    }

    fun acceptableMediaType(vararg availables: Pair<String, String>) = acceptableMediaType(availables.toList())
}
