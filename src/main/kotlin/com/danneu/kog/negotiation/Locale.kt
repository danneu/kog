package com.danneu.kog.negotiation

import com.danneu.kog.Header
import com.danneu.kog.Request
import com.danneu.kog.toy

enum class Locale {
    // Special
    Wildcard,

    // Locales
    Germany,
    Argentina,
    Australia,
    Belize,
    Brazil,
    Liechtenstein,
    Luxembourg,
    Switzerland,
    Bolivia,
    Canada,
    Caribbean,
    Chile,
    Colombia,
    CostaRica,
    DominicanRepublic,
    Ecuador,
    ElSalvador,
    Finland,
    Guatemala,
    Honduras,
    Ireland,
    Romania,
    Jamaica,
    Mexico,
    NewZealand,
    Nicaragua,
    Panama,
    Paraguay,
    Peru,
    Philippines,
    Portugal,
    PuertoRico,
    SouthAfrica,
    Spain,
    Austria,
    TrinidadAndTobago,
    UnitedKingdom,
    UnitedStates,
    Uruguay,
    Venezuela,
    Zimbabwe
    ;

    companion object {
        fun fromString(input: String): Locale? = when (input.toUpperCase()) {
            "AR" -> Argentina
            "LU" -> Luxembourg
            "AU" -> Australia
            "BO" -> Bolivia
            "BR" -> Brazil
            "RO" -> Romania
            "BZ" -> Belize
            "CA" -> Canada
            "CB" -> Caribbean
            "CL" -> Chile
            "CO" -> Colombia
            "CR" -> CostaRica
            "DO" -> DominicanRepublic
            "EC" -> Ecuador
            "ES" -> Spain
            "GB" -> UnitedKingdom
            "GT" -> Guatemala
            "FI" -> Finland
            "HN" -> Honduras
            "IE" -> Ireland
            "JM" -> Jamaica
            "MX" -> Mexico
            "NI" -> Nicaragua
            "NZ" -> NewZealand
            "PA" -> Panama
            "PE" -> Peru
            "PH" -> Philippines
            "PR" -> PuertoRico
            "PT" -> Portugal
            "PY" -> Paraguay
            "SV" -> ElSalvador
            "TT" -> TrinidadAndTobago
            "US" -> UnitedStates
            "DE" -> Germany
            "LI" -> Liechtenstein
            "UY" -> Uruguay
            "VE" -> Venezuela
            "ZA" -> SouthAfrica
            "ZW" -> Zimbabwe
            "AT" -> Austria
            "CH" -> Switzerland
            else -> null
        }
    }
}

enum class AvailableLang {
    Spanish,
    English
}

fun Request.lang(): AvailableLang {
    val availableLangs = listOf(
        Lang.Spanish(),
        Lang.English()
    )

    return when (this.negotiate.acceptableLanguage(availableLangs)) {
        Lang.English() -> AvailableLang.English
        else -> AvailableLang.Spanish
    }
}

fun main(args: Array<String>) {

    //val neg = Negotiator(Request.toy(headers = mutableListOf(Header.AcceptLanguage to "en-US, es")))
    val req = Request.toy(headers = mutableListOf(Header.AcceptLanguage to "es, en-US, es"))

    println(req.lang())
}
