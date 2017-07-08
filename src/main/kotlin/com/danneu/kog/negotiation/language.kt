package com.danneu.kog.negotiation

import kotlin.comparisons.compareBy

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

interface Locale {
    val code: String

    object Codes {
        const val Argentina = "ar"
        const val Australia = "au"
        const val Austria = "at"
        const val Belgium = "be"
        const val Belize = "bz"
        const val Bolivia = "bo"
        const val Brazil = "br"
        const val Canada = "ca"
        const val Caribbean = "cb"
        const val Chile = "cl"
        const val Colombia = "co"
        const val CostaRica = "cr"
        const val DominicanRepublic = "do"
        const val Ecuador = "ec"
        const val ElSalvador = "sv"
        const val Finland = "fi"
        const val France = "fr"
        const val Germany = "de"
        const val Guatemala = "gt"
        const val Honduras = "hn"
        const val Ireland = "ie"
        const val Italy = "it"
        const val Jamaica = "jm"
        const val Liechtenstein = "li"
        const val Luxembourg = "lu"
        const val Mexico = "mx"
        const val Monaco = "mc"
        const val Netherlands = "nl"
        const val NewZealand = "nz"
        const val Nicaragua = "ni"
        const val Norway = "no"
        const val Panama = "pa"
        const val Paraguay = "py"
        const val Peru = "pe"
        const val Philippines = "ph"
        const val Portugal = "pt"
        const val PuertoRico = "pr"
        const val SouthAfrica = "za"
        const val Spain = "es"
        const val Sweden = "se"
        const val Switzerland = "ch"
        const val TrinidadAndTobago = "tt"
        const val UnitedKingdom = "gb"
        const val UnitedStates = "us"
        const val Uruguay = "uy"
        const val Venezuela = "ve"
        const val Zimbabwe = "zw"
    }

    enum class English(override val code: String) : Locale {
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

    enum class French(override val code: String) : Locale {
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

    enum class Dutch(override val code: String) : Locale {
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

    enum class Portuguese(override val code: String) : Locale {
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

    enum class Sami(override val code: String) : Locale {
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

    enum class German(override val code: String) : Locale {
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

    enum class Italian(override val code: String) : Locale {
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

    enum class Spanish(override val code: String) : Locale {
        Argentina(Codes.Argentina),
        Bolivia(Codes.Bolivia),
        Chile(Codes.Chile),
        Colombia(Codes.Colombia),
        CostaRica(Codes.CostaRica),
        DominicanRepublic(Codes.DominicanRepublic),
        Ecuador(Codes.Ecuador),
        ElSalvador(Codes.ElSalvador),
        Guatemala(Codes.Guatemala),
        Honduras(Codes.Honduras),
        Mexico(Codes.Mexico),
        Nicaragua(Codes.Nicaragua),
        Panama(Codes.Panama),
        Paraguay(Codes.Paraguay),
        Peru(Codes.Peru),
        PuertoRico(Codes.PuertoRico),
        Spain(Codes.Spain),
        Uruguay(Codes.Uruguay),
        Venezuela(Codes.Venezuela);

        companion object {
            fun fromCode(code: String?): Spanish? = when (code?.toLowerCase()) {
                Codes.Argentina -> Argentina
                Codes.Bolivia -> Bolivia
                Codes.Chile -> Chile
                Codes.Colombia -> Colombia
                Codes.CostaRica -> CostaRica
                Codes.DominicanRepublic -> DominicanRepublic
                Codes.Ecuador -> Ecuador
                Codes.ElSalvador -> ElSalvador
                Codes.Guatemala -> Guatemala
                Codes.Honduras -> Honduras
                Codes.Mexico -> Mexico
                Codes.Nicaragua -> Nicaragua
                Codes.Panama -> Panama
                Codes.Paraguay -> Paraguay
                Codes.Peru -> Peru
                Codes.PuertoRico -> PuertoRico
                Codes.Spain -> Spain
                Codes.Uruguay -> Uruguay
                Codes.Venezuela -> Venezuela
                else -> null
            }
        }
    }
}

sealed class Lang(val prefixCode: String, prettyName: String, val locale: Locale? = null) {
    val code = prefixCode + if (locale != null) "-${locale.code.toUpperCase()}" else ""
    val prettyName = prettyName + if (locale == null) "[*]" else "[${locale.code.toUpperCase()}]"

    // Special
    object Wildcard: Lang("*", "*")
    // Languages (without locales)
    class Afrikaans: Lang(Codes.Afrikaans, "Afrikaans")
    class Esperanto: Lang(Codes.Esperanto, "Esperanto")
    class Finnish: Lang(Codes.Finnish, "Finnish")
    class Klingon: Lang(Codes.Klingon, "Klingon")
    class Norwegian: Lang(Codes.Norwegian, "Norwegian")
    class Romanian: Lang(Codes.Romanian, "Romanian")
    // Languages (with locales
    class Dutch(locale: Locale.Dutch? = null) : Lang(Codes.Dutch, "Dutch", locale)
    class English(locale: Locale.English? = null) : Lang(Codes.English, "English", locale)
    class French(locale: Locale.French? = null) : Lang(Codes.French, "French", locale)
    class German(locale: Locale.German? = null) : Lang(Codes.German, "German", locale)
    class Italian(locale: Locale.Italian? = null) : Lang(Codes.Italian, "Italian", locale)
    class Portuguese(locale: Locale.Portuguese? = null) : Lang(Codes.Portuguese, "Portuguese", locale)
    class Sami(locale: Locale.Sami? = null) : Lang(Codes.Sami, "Sami", locale)
    class Spanish(locale: Locale.Spanish? = null) : Lang(Codes.Spanish, "Spanish", locale)

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is Lang -> this.code.toLowerCase() == other.code.toLowerCase()
            else -> false
        }
    }

    override fun hashCode() = this.code.toLowerCase().hashCode()

    override fun toString() = prettyName

    object Codes {
        // Special
        val Wildcard = "*"
        // Languages
        val Afrikaans = "af"
        val Dutch = "nl"
        val English = "en"
        val Esperanto = "eo"
        val Finnish = "fi"
        val French = "fr"
        val German = "de"
        val Italian = "it"
        val Klingon = "tlh"
        val Norwegian = "no"
        val Portuguese = "pt"
        val Romanian = "ro"
        val Sami = "se"
        val Spanish = "es"
    }

    companion object {
        fun fromCode(code: String): Lang? {
            code.split("-").let { parts ->
                return fromPrefix(parts[0], parts.getOrNull(1))
            }
        }

        fun fromPrefix(prefix: String, localeCode: String? = null): Lang? {
            return when (prefix.toLowerCase()) {
                Codes.Wildcard -> Wildcard
                Codes.Afrikaans -> Afrikaans()
                Codes.Dutch -> Dutch(Locale.Dutch.fromCode(localeCode))
                Codes.English -> English(Locale.English.fromCode(localeCode))
                Codes.Esperanto -> Esperanto()
                Codes.Finnish -> Finnish()
                Codes.French -> French(Locale.French.fromCode(localeCode))
                Codes.German -> German(Locale.German.fromCode(localeCode))
                Codes.Italian -> Italian(Locale.Italian.fromCode(localeCode))
                Codes.Klingon -> Klingon()
                Codes.Norwegian -> Norwegian()
                Codes.Portuguese -> Portuguese(Locale.Portuguese.fromCode(localeCode))
                Codes.Romanian -> Romanian()
                Codes.Sami -> Sami(Locale.Sami.fromCode(localeCode))
                Codes.Spanish -> Spanish(Locale.Spanish.fromCode(localeCode))
                else -> null
            }
        }
    }
}

// This class is just a pair of language and its q-value.
//
// TODO: Maybe AcceptLanguage should be Pair<Lang, QValue>. Lang vs AcceptLanguage is confusing as top-level classes.
class AcceptLanguage(val lang: Lang, val q: Double = 1.0) {
    override fun toString() = "${lang.code};q=$q"

    override fun equals(other: Any?) = when (other) {
        is AcceptLanguage -> this.lang == other.lang && this.q == other.q
        else -> false
    }

    // Generated by IDEA
    override fun hashCode() = 31 * lang.hashCode() + q.hashCode()

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


