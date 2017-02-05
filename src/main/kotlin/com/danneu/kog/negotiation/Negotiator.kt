package com.danneu.kog.negotiation

import com.danneu.kog.Header
import com.danneu.kog.Request

// The negotiator takes a request and determines the client's best-fitting preference from a list of available options
// by querying the request's Accept and Accept-* headers.

class Negotiator(val request: Request) {

    // ACCEPT-ENCODING NEGOTIATION

    /** List of the preferred encodings send by client
     */
    fun encodings(): List<Encoding> {
        val header = request.getHeader(Header.AcceptEncoding) ?: return listOf(Encoding("identity"))
        return Encoding.parse(header).let { Encoding.prioritize(it) }
    }

    /** Returns the client's most-preferred encoding
     */
    fun acceptableEncoding(availables: List<String>): String? {
        return encodings().find { encoding ->
            availables.any { avail -> encoding.acceptable(avail) }
        }?.name
    }

    // ACCEPT-LANGUAGE NEGOTIATION

    // FIXME: This section is pretty messy

    private fun allAcceptLanguages(): List<AcceptLanguage> {
        val header = request.getHeader(Header.AcceptLanguage) ?: return emptyList()
        return AcceptLanguage.parseHeader(header)
            .let { AcceptLanguage.prioritize(it) }
            .distinctBy { it.lang.code } // TODO: why doesn't distinctBy { it.lang } work?
    }

    /** List of preferred languages sent by the client
     */
    fun languages(): List<Lang> {
        return allAcceptLanguages()
            .filter { it.q > 0 }
            .map { it.lang }
    }

    /** The filtered list of our available languages, client's most-preferred coming first.
     */
    fun acceptableLanguages(availables: List<Lang>): List<Lang> {
        allAcceptLanguages().let { acceptLang ->
            val excludedLangs: Set<Lang> = acceptLang.filter { it.q == 0.0 }.map { it.lang }.toSet()
            val langs: List<Lang> = acceptLang.filter { it.q > 0.0 }.map { it.lang }
                .let { if (it.isEmpty() && excludedLangs.isEmpty()) listOf(Lang.Wildcard) else it }

            if (langs.isEmpty()) return emptyList()

            // explicit langs are langs from our availability list that the client specifically request.
            // i.e. no wildcard matches, no non-locale matches like en == en_GB
            val explicit: List<Lang> = langs.mapNotNull { lang ->
                availables.filter { avail ->
                    AcceptLanguage.acceptable(lang, avail, excludedLangs)
                }.sortedBy { if (lang.locale == it.locale) -1 else 0 }
                .firstOrNull()
            }

            // implicit langs were matched with wildcard or fuzzy locale
            val implicit: List<Lang> = availables
                // ignore the ones already in the explicit list
                .filterNot { it in explicit }
                // filter the ones that are acceptable, they'll come after the explicits
                .filter { avail ->
                    langs.any { lang ->
                        AcceptLanguage.acceptable(lang, avail, excludedLangs)
                    }
                }

            // TODO: Why do we have dupes?
            return (explicit + implicit).distinct()
        }
    }

    /** The client's most preferred language in our available list.
     */
    fun acceptableLanguage(availables: List<Lang>): Lang? = acceptableLanguages(availables).firstOrNull()

    // ACCEPT NEGOTIATION

    /** List of preferred mediaTypes sent by the client
     */
    fun mediaTypes(): List<MediaType> {
        val header = request.getHeader(Header.Accept) ?: return emptyList()
        return MediaType.parseHeader(header).distinct().let { MediaType.prioritize(it) }
    }

    /** Returns our available media type that the user most prefers
     */
    fun acceptableMediaType(availables: Collection<Pair<String, String>>): Pair<String, String>? {
        mediaTypes().let { types ->
            if (types.isEmpty()) return availables.firstOrNull()
            return availables.find { avail ->
                types.any { type -> type.acceptable(avail) }
            }
        }
    }

    fun acceptableMediaType(vararg availables: Pair<String, String>) = acceptableMediaType(availables.toList())
}
