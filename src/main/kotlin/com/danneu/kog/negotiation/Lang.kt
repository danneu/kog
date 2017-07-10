package com.danneu.kog.negotiation

// TODO: Finish implementing http://www.lingoes.net/en/translator/langcode.htm

sealed class Lang(val name: String, val locale: Locale = Locale.Wildcard) {
    companion object {
        fun fromString(input: String): Lang? {
            val (prefix, localeString) = input.split("-", limit = 2)

            val locale = if (localeString.isEmpty()) {
                Locale.Wildcard
            } else {
                Locale.fromString(localeString)
            }

            // TODO: Decide how to handle languages with unknown locales. For now, they are skipped
            locale ?: return null

            return when (prefix) {
                "*" -> Wildcard
                "en" -> English(locale)
                "es" -> Spanish(locale)
                "de" -> German(locale)
                "it" -> Italian(locale)
                "fr" -> French(locale)
                "pt" -> Portuguese(locale)
                "no" -> Norwegian(locale)
                "se" -> Sami(locale)
                "fi" -> Finnish(locale)
                "ro" -> Romanian(locale)
                "nl" -> Dutch(locale)
                else -> null
            }
        }
    }

    // Necessary for .distinctBy to work
    override fun hashCode() = 31 * name.hashCode() + locale.hashCode()

    override fun toString() = if (locale == Locale.Wildcard) {
        "$name[*]"
    } else {
        "$name[$locale]"
    }

    override fun equals(other: Any?): Boolean {
        return other is Lang && name == other.name && locale == other.locale
    }

    object Wildcard : Lang("Wildcard")
    class English (locale: Locale = Locale.Wildcard) : Lang("English", locale)
    class Spanish (locale: Locale = Locale.Wildcard) : Lang("Spanish", locale)
    class German (locale: Locale = Locale.Wildcard) : Lang("German", locale)
    class French (locale: Locale = Locale.Wildcard) : Lang("French", locale)
    class Italian (locale: Locale = Locale.Wildcard) : Lang("Italian", locale)
    class Portuguese (locale: Locale = Locale.Wildcard) : Lang("Portuguese", locale)
    class Norwegian (locale: Locale = Locale.Wildcard) : Lang("Norwegian", locale)
    class Sami (locale: Locale = Locale.Wildcard) : Lang("Sami", locale)
    class Finnish (locale: Locale = Locale.Wildcard) : Lang("Finnish", locale)
    class Romanian (locale: Locale = Locale.Wildcard) : Lang("Romanian", locale)
    class Dutch (locale: Locale = Locale.Wildcard) : Lang("Dutch", locale)
}
