package com.danneu.kog.negotiation

// A **goofy** wrapper around accept-language prefix/suffix pairs like en, en-GB, en-US.
//
// The implementation got a bit gnarly since I was reverse-engineering how it should work from
// other server test cases and letting TDD drive my impl like a black box in some parts.
//
// https://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html
//
// TODO: Incomplete, experimental.
// TODO: Finish language code list http://www.lingoes.net/en/translator/langcode.htm
// TODO: Maybe should keep it open-ended like any language being able to use any locale.
// TODO: Maybe should just simplify it into Pair("en", null), Pair("en", "US") style stuff.


interface Suffix {
    val code: String
}

object Locale {
    object Codes {
        val Australia = "au"
        val Belgium = "be"
        val Belize = "bz"
        val Canada = "ca"
        val Caribbean = "cb"
        val UnitedKingdom = "gb"
        val Ireland = "ie"
        val France = "fr"
        val Jamaica = "jm"
        val NewZealand = "nz"
        val Philippines = "ph"
        val TrinidadAndTobago = "tt"
        val UnitedStates = "us"
        val SouthAfrica = "za"
        val Switzerland = "ch"
        val Zimbabwe = "zw"
        val Luxembourg = "lu"
        val Monaco = "mc"
        val Austria = "at"
        val Liechtenstein = "li"
        val Germany = "de"
        val Italy = "it"
        val Brazil = "br"
        val Portugal = "pt"
        val Finland = "fi"
        val Norway = "no"
        val Sweden = "se"
        val Netherlands = "nl"
    }

    enum class English(override val code: String) : Suffix {
        Australia(Codes.Australia),
        Belize(Codes.Belize),
        Canada(Codes.Canada),
        Caribbean(Codes.Caribbean),
        Ireland(Codes.Ireland),
        Jamaica(Codes.Jamaica),
        NewZealand(Codes.NewZealand),
        Philippines(Codes.Philippines),
        SouthAfrica(Codes.SouthAfrica),
        TrinidadAndTobago(Codes.TrinidadAndTobago),
        UnitedKingdom(Codes.UnitedKingdom),
        UnitedStates(Codes.UnitedStates),
        Zimbabwe(Codes.Zimbabwe);

        companion object {
            fun fromCode(code: String?): English? = when (code?.toLowerCase()) {
                Codes.Australia -> Australia
                Codes.Belize -> Belize
                Codes.Canada -> Canada
                Codes.Caribbean -> Caribbean
                Codes.Ireland -> Ireland
                Codes.Jamaica -> Jamaica
                Codes.NewZealand -> NewZealand
                Codes.Philippines -> Philippines
                Codes.SouthAfrica -> SouthAfrica
                Codes.TrinidadAndTobago -> TrinidadAndTobago
                Codes.UnitedKingdom -> UnitedKingdom
                Codes.UnitedStates -> UnitedStates
                Codes.Zimbabwe -> Zimbabwe
                else -> null
            }
        }
    }

    enum class French(override val code: String) : Suffix {
        Belgium(Codes.Belgium),
        Canada(Codes.Canada),
        France(Codes.France),
        Luxembourg(Codes.Luxembourg),
        Monaco(Codes.Monaco),
        Switzerland(Codes.Switzerland);

        companion object {
            fun fromCode(code: String?): French? = when (code?.toLowerCase()) {
                Codes.Belgium -> Belgium
                Codes.Canada -> Canada
                Codes.France -> France
                Codes.Luxembourg -> Luxembourg
                Codes.Monaco -> Monaco
                Codes.Switzerland -> Switzerland
                else -> null
            }
        }
    }

    enum class Dutch(override val code: String) : Suffix {
        Belgium(Codes.Belgium),
        Netherlands(Codes.Netherlands);

        companion object {
            fun fromCode(code: String?): Dutch? = when (code?.toLowerCase()) {
                Codes.Belgium -> Belgium
                Codes.Netherlands -> Netherlands
                else -> null
            }
        }
    }

    enum class Portuguese(override val code: String) : Suffix {
        Brazil(Codes.Brazil),
        Portugal(Codes.Portugal);

        companion object {
            fun fromCode(code: String?): Portuguese? = when (code?.toLowerCase()) {
                Codes.Brazil -> Brazil
                Codes.Portugal -> Portugal
                else -> null
            }
        }
    }

    enum class Sami(override val code: String) : Suffix {
        Finland(Codes.Finland),
        Norway(Codes.Norway),
        Sweden(Codes.Sweden);

        companion object {
            fun fromCode(code: String?): Sami? = when (code?.toLowerCase()) {
                Codes.Finland -> Finland
                Codes.Norway -> Norway
                Codes.Sweden -> Sweden
                else -> null
            }
        }
    }

    enum class German(override val code: String) : Suffix {
        Austria(Codes.Austria),
        Switzerland(Codes.Switzerland),
        Germany(Codes.Germany),
        Liechtenstein(Codes.Liechtenstein),
        Luxembourg(Codes.Luxembourg);

        companion object {
            fun fromCode(code: String?): German? = when (code?.toLowerCase()) {
                Codes.Austria -> Austria
                Codes.Switzerland -> Switzerland
                Codes.Germany -> Germany
                Codes.Liechtenstein -> Liechtenstein
                Codes.Luxembourg -> Luxembourg
                else -> null
            }
        }
    }

    enum class Italian(override val code: String) : Suffix {
        Switzerland(Codes.Switzerland),
        Italy(Codes.Italy);

        companion object {
            fun fromCode(code: String?): Italian? = when (code?.toLowerCase()) {
                Codes.Switzerland -> Switzerland
                Codes.Italy -> Italy
                else -> null
            }
        }
    }
}

sealed class Lang(val prefixCode: String, prettyName: String, val locale: Suffix? = null) {
    val code = prefixCode + if (locale != null) "-${locale.code.toUpperCase()}" else ""
    val prettyName = prettyName + if (locale != null) " (${locale.code.toUpperCase()})" else ""

    // Special
    object Wildcard: Lang("*", "*")
    // Languages
    class Afrikaans: Lang("af", "Afrikaans")
    class Dutch(locale: Locale.Dutch? = null): Lang("nl", "Dutch", locale)
    class English(locale: Locale.English? = null): Lang("en", "English", locale)
    class Esperanto: Lang("eo", "Esperanto")
    class Finnish: Lang("fi", "Finnish")
    class French(locale: Locale.French? = null): Lang("fr", "French", locale)
    class German(locale: Locale.German? = null): Lang("de", "German", locale)
    class Italian(locale: Locale.Italian? = null): Lang("it", "Italian", locale)
    class Klingon: Lang("tlh", "Klingon")
    class Norwegian: Lang("no", "Norwegian")
    class Portuguese(locale: Locale.Portuguese? = null): Lang("pt", "Portuguese", locale)
    class Romanian: Lang("ro", "Romanian")
    class Sami(locale: Locale.Sami? = null): Lang("se", "Sami", locale)
    class Spanish: Lang("es", "Spanish")

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is Lang -> this.code.toLowerCase() == other.code.toLowerCase()
            else -> false
        }
    }

    override fun toString() = code

    companion object {
        fun fromCode(code: String): Lang? {
            code.split("-").let { parts ->
                return fromPrefix(parts[0], parts.getOrNull(1))
            }
        }

        fun fromPrefix(prefix: String, localeCode: String? = null): Lang? {
            return when (prefix.toLowerCase()) {
                "*" -> Wildcard
                "af" -> Afrikaans()
                "nl" -> Dutch(Locale.Dutch.fromCode(localeCode))
                "en" -> English(Locale.English.fromCode(localeCode))
                "eo" -> Esperanto()
                "fi" -> Finnish()
                "fr" -> French(Locale.French.fromCode(localeCode))
                "de" -> German(Locale.German.fromCode(localeCode))
                "it" -> Italian(Locale.Italian.fromCode(localeCode))
                "tlh" -> Klingon()
                "no" -> Norwegian()
                "pt" -> Portuguese(Locale.Portuguese.fromCode(localeCode))
                "ro" -> Romanian()
                "se" -> Sami(Locale.Sami.fromCode(localeCode))
                "es" -> Spanish()
                else -> null
            }
        }
    }
}

// This class is just a pair of language and it's q-value.
//
// IDEA: Maybe AcceptLanguage should be Pair<Lang, QValue>
class AcceptLanguage(val lang: Lang, val q: Double = 1.0) {
    override fun toString() = "${lang.code};q=$q"

    override fun equals(other: Any?) = when (other) {
        is AcceptLanguage -> this.lang == other.lang && this.q == other.q
        else -> false
    }

    companion object {
        val regex = Regex("""^\s*([^\s\-;]+)(?:-([^\s;]+))?\s*(?:;(.*))?$""")

        // TODO: Test malformed header

        fun acceptable(clientLang: Lang, availableLang: Lang, excludedLangs: Set<Lang>): Boolean {
            if (availableLang in excludedLangs) return false
            // clientLang is * so everything is acceptable
            if (clientLang == Lang.Wildcard) return true
            // short-circuit if they are equal (en == en, en-gb == en-gb, en-gb !== en)
            if (clientLang == availableLang) return true
            if (clientLang.prefixCode == availableLang.prefixCode) return true

            return false
        }

        /** Parses a single segment pair
         */
        fun parse(string: String): AcceptLanguage? {
            val parts = regex.find(string)?.groupValues?.drop(1) ?: return null
            val lang = Lang.fromPrefix(parts[0], parts[1]) ?: return null
            val q = QValue.parse(parts[2]) ?: 1.0
            return AcceptLanguage(lang, q)
        }


        /** Parses comma-delimited string of types
         */
        fun parseHeader(header: String): List<AcceptLanguage> {
            return header.split(",").map(String::trim).mapNotNull(this::parse)
        }

        fun prioritize(xs: List<AcceptLanguage>): List<AcceptLanguage> {
            return xs.sortedWith(compareBy(
                { -it.q },
                // Wildcard comes last
                { when (it.lang) {
                    Lang.Wildcard ->
                        // TODO: Why doesn't -1 work? I would've thought this would put wildcards in the front.
                        1
                    else ->
                        0
                }}
            ))

        }
    }
}


