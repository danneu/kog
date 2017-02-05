package com.danneu.kog

import com.danneu.kog.Header.Accept
import com.danneu.kog.Header.AcceptEncoding
import com.danneu.kog.negotiation.AcceptLanguage
import com.danneu.kog.negotiation.Encoding
import com.danneu.kog.negotiation.Lang
import com.danneu.kog.negotiation.Locale
import com.danneu.kog.negotiation.MediaType
import com.danneu.kog.negotiation.Negotiator
import org.junit.Assert.*
import org.junit.Test


fun codeList(vararg codes: String): List<Lang> {
    return codes.map { Lang.fromCode(it)!! }
}

class LanguageTests {
    @Test
    fun testPrioritize() {
        val unsorted = listOf(AcceptLanguage(Lang.Wildcard), AcceptLanguage(Lang.English()))
        val sorted = AcceptLanguage.prioritize(unsorted)
        val expected = listOf(AcceptLanguage(Lang.English()), AcceptLanguage(Lang.Wildcard))
        assertEquals("wildcard is always moved to the end", expected, sorted)
    }
    @Test
    fun testMissingHeader() {
        val neg = Negotiator(Request.toy(headers = mutableListOf()))
        assertEquals(listOf<Lang>(), neg.languages())
        // TODO: Do I want Wildcard to appear in languages() if it's not set? Or should languages()
        // only reflect what the user explicitly sent?
        // assertEquals("missing header becomes *", listOf(Lang.Wildcard), neg.languages())
    }

    @Test
    fun testWildcard() {
        val req = Request.toy(headers = mutableListOf(Header.AcceptLanguage to "*"))
        val neg = Negotiator(req)
        assertEquals("parses *", listOf(Lang.Wildcard), neg.languages())
    }

    @Test
    fun testParsing() {
        run {
            val neg = Negotiator(Request.toy(headers = mutableListOf(Header.AcceptLanguage to "*, en")))
            // TODO: Should languags() reorder wildcard to the back, or should it be done internally?
            // assertEquals(listOf(Lang.Wildcard, Lang.English()), neg.languages())

            // For now, let's make languages() reorder wildcard to the back
            assertEquals(listOf(Lang.English(), Lang.Wildcard), neg.languages())
        }
        run {
            val neg = Negotiator(Request.toy(headers = mutableListOf(Header.AcceptLanguage to "*, en;q=0")))
            assertEquals("q=0 gets removed", listOf(Lang.Wildcard), neg.languages())
        }
        run {
            val neg = Negotiator(Request.toy(headers = mutableListOf(Header.AcceptLanguage to "*;q=0.8, en, es")))
            assertEquals("if q < 1, it gets sorted later", listOf(Lang.English(), Lang.Spanish(), Lang.Wildcard), neg.languages())
        }
        run {
            val neg = Negotiator(Request.toy(headers = mutableListOf(Header.AcceptLanguage to "en")))
            assertEquals(listOf(Lang.English()), neg.languages())
        }
        run {
            val neg = Negotiator(Request.toy(headers = mutableListOf(Header.AcceptLanguage to "en;q=0")))
            assertEquals(emptyList<Lang>(), neg.languages())
        }
        run {
            val neg = Negotiator(Request.toy(headers = mutableListOf(Header.AcceptLanguage to "en;q=0.8, es")))
            assertEquals(listOf(Lang.Spanish(), Lang.English()), neg.languages())
        }
        run {
            val neg = Negotiator(Request.toy(headers = mutableListOf(Header.AcceptLanguage to "en;q=0.9, es;q=0.8, en;q=0.7")))
            assertEquals(listOf(Lang.English(), Lang.Spanish()), neg.languages())
        }
        run {
            val neg = Negotiator(Request.toy(headers = mutableListOf(Header.AcceptLanguage to "en-US, en;q=0.8")))
            assertEquals(codeList("en-US", "en"), neg.languages())
            assertEquals(codeList("en-US", "en"), neg.languages())
        }
        run {
            val neg = Negotiator(Request.toy(headers = mutableListOf(Header.AcceptLanguage to "en-US, en-GB")))
            assertEquals(codeList("en-US", "en-GB"), neg.languages())
        }
        run {
            val neg = Negotiator(Request.toy(headers = mutableListOf(Header.AcceptLanguage to "en-US;q=0.8, es")))
            assertEquals(codeList("es", "en-US"), neg.languages())
        }
        run {
            val neg = Negotiator(Request.toy(headers = mutableListOf(Header.AcceptLanguage to "nl;q=0.5, fr, de, en, it, es, pt, no, se, fi, ro")))
            assertEquals(listOf(
                Lang.French(),
                Lang.German(),
                Lang.English(),
                Lang.Italian(),
                Lang.Spanish(),
                Lang.Portuguese(),
                Lang.Norwegian(),
                Lang.Sami(),
                Lang.Finnish(),
                Lang.Romanian(),
                Lang.Dutch()
            ), neg.languages())
        }
    }

    @Test
    fun testAcceptableLanguages() {
        run {
            val msg = "should return original list"
            val neg = Negotiator(Request.toy(headers = mutableListOf()))
            assertEquals(msg, listOf(Lang.English()), neg.acceptableLanguages(listOf(Lang.English())))
            assertEquals(msg, listOf(Lang.Spanish(), Lang.English()), neg.acceptableLanguages(listOf(Lang.Spanish(), Lang.English())))
        }
        run {
            val msg = "should return original list"
            val neg = Negotiator(Request.toy(headers = mutableListOf(Header.AcceptLanguage to "*")))
            assertEquals(msg, listOf(Lang.English()), neg.acceptableLanguages(listOf(Lang.English())))
            assertEquals(msg, listOf(Lang.Spanish(), Lang.English()), neg.acceptableLanguages(listOf(Lang.Spanish(), Lang.English())))
        }
        run {
            val msg = "should return list in client-preferred order"
            val neg = Negotiator(Request.toy(headers = mutableListOf(Header.AcceptLanguage to "*, en")))
            assertEquals(msg, listOf(Lang.English()), neg.acceptableLanguages(listOf(Lang.English())))
            assertEquals(msg, listOf(Lang.English(), Lang.Spanish()), neg.acceptableLanguages(listOf(Lang.Spanish(), Lang.English())))
        }
        run {
            val msg = "excludes en"
            val neg = Negotiator(Request.toy(headers = mutableListOf(Header.AcceptLanguage to "*, en;q=0")))
            assertEquals(msg, listOf<Lang>(), neg.acceptableLanguages(listOf(Lang.English())))
            assertEquals(msg, listOf(Lang.Spanish()), neg.acceptableLanguages(listOf(Lang.Spanish(), Lang.English())))
        }
        run {
            val msg = "returns preferred languages"
            val neg = Negotiator(Request.toy(headers = mutableListOf(Header.AcceptLanguage to "*;q=0.8, en, es")))
            assertEquals(msg,
                codeList( "en", "es", "fr", "de", "it", "pt", "no", "se", "fi", "ro", "nl" ),
                neg.acceptableLanguages(codeList("fr", "de", "en", "it", "es", "pt", "no", "se", "fi", "ro", "nl" ))
            )
        }
        run {
            val msg = "returns preferred languages"
            val neg = Negotiator(Request.toy(headers = mutableListOf(Header.AcceptLanguage to "en")))
            assertEquals(msg, listOf(Lang.English()), neg.acceptableLanguages(listOf(Lang.English())))
            assertEquals(msg, listOf(Lang.English()), neg.acceptableLanguages(listOf(Lang.English(), Lang.Spanish())))
            assertEquals(msg, listOf(Lang.English()), neg.acceptableLanguages(listOf(Lang.Spanish(), Lang.English())))
        }
        run {
            val msg = "accepts en-US, preferring en over en-US"
            val neg = Negotiator(Request.toy(headers = mutableListOf(Header.AcceptLanguage to "en")))
            assertEquals(msg, listOf(Lang.English(Locale.English.UnitedStates)), neg.acceptableLanguages(listOf(Lang.English(Locale.English.UnitedStates))))
            assertEquals(msg, listOf(Lang.English(Locale.English.UnitedStates)), neg.acceptableLanguages(listOf(Lang.English(Locale.English.UnitedStates))))
            assertEquals(msg, listOf(Lang.English(), Lang.English(Locale.English.UnitedStates)), neg.acceptableLanguages(listOf(Lang.English(Locale.English.UnitedStates), Lang.English())))
            assertEquals(msg, listOf(Lang.English(), Lang.English(Locale.English.UnitedStates)), neg.acceptableLanguages(listOf(Lang.English(), Lang.English(Locale.English.UnitedStates))))
        }
        run {
            val msg = "returns nothing"
            val neg = Negotiator(Request.toy(headers = mutableListOf(Header.AcceptLanguage to "en;q=0")))
            assertEquals(msg, listOf<Lang>(), neg.acceptableLanguages(listOf(Lang.English())))
            assertEquals(msg, listOf<Lang>(), neg.acceptableLanguages(listOf(Lang.English(), Lang.Spanish())))
        }
        run {
            val msg = "returns preferred languages"
            val neg = Negotiator(Request.toy(headers = mutableListOf(Header.AcceptLanguage to "en;q=0.8, es")))
            assertEquals(msg, listOf(Lang.English()), neg.acceptableLanguages(listOf(Lang.English())))
            assertEquals(msg, listOf(Lang.Spanish(), Lang.English()), neg.acceptableLanguages(listOf(Lang.English(), Lang.Spanish())))
            assertEquals(msg, listOf(Lang.Spanish(), Lang.English()), neg.acceptableLanguages(listOf(Lang.Spanish(), Lang.English())))
        }


        run {
            val msg = "returns preferred languages"
            val neg = Negotiator(Request.toy(headers = mutableListOf(Header.AcceptLanguage to "en;q=0.9, es;q=0.8, en;q=0.7")))
            assertEquals(msg, codeList("en"), neg.acceptableLanguages(codeList("en")))
            // TODO: apparently the later dupe overwrites the q of the earlier?
            //assertEquals(msg, codeList("es", "en"), neg.acceptableLanguages(codeList("en", "es")))
            //assertEquals(msg, codeList("es", "en"), neg.acceptableLanguages(codeList("es", "en")))
        }

        run {
            val neg = Negotiator(Request.toy(headers = mutableListOf(Header.AcceptLanguage to "en-US, en;q=0.8")))
            assertEquals("prefers en-US over en", codeList("en-US", "en"), neg.acceptableLanguages(codeList("en-US", "en")))
            assertEquals("prefers en-US over en", codeList("en-US", "en", "en-GB"), neg.acceptableLanguages(codeList("en-GB", "en-US", "en")))
        }

        run {
            val neg = Negotiator(Request.toy(headers = mutableListOf(Header.AcceptLanguage to "en-US, en-GB")))
            assertEquals(codeList("en-US", "en-GB"), neg.acceptableLanguages(codeList("en-US", "en-GB")))
            assertEquals(codeList("en-US", "en-GB"), neg.acceptableLanguages(codeList("en-GB", "en-US")))
        }

        run {
            val msg = "prefers es over en-US"
            val neg = Negotiator(Request.toy(headers = mutableListOf(Header.AcceptLanguage to "en-US;q=0.8, es")))
            assertEquals(msg, codeList("es", "en"), neg.acceptableLanguages(codeList("en", "es")))
        }

        run {
            val msg = "returns preferred languages"
            val neg = Negotiator(Request.toy(headers = mutableListOf(Header.AcceptLanguage to "nl;q=0.5, fr, de, en, it, es, pt, no, se, fi, ro")))
            assertEquals(msg,
                codeList("fr", "de", "en", "it", "es", "pt", "no", "se", "fi", "ro", "nl"),
                neg.acceptableLanguages(codeList("fr", "de", "en", "it", "es", "pt", "no", "se", "fi", "ro", "nl"))
            )
        }
    }
}

class MediaTypeTests {
    @Test
    fun testDuplication() {
        assertEquals(
            "dupes are removed",
            listOf(MediaType("a", "b", 0.5)),
            Negotiator(Request.toy().setHeader(Accept, "a/b;q=0.5, a/b;q=0.5, a/b;q=0.5")).mediaTypes()
        )
    }

    @Test
    fun testPrioritize() {
        assertEquals(
            "more specific ranges should come before less specific ranges",
            listOf(
                MediaType("foo", "bar"),
                MediaType("foo", "*"),
                MediaType("*", "*"),
                MediaType("foo", "*", 0.5),
                MediaType("foo", "bar", 0.2),
                MediaType("*", "*", 0.1)
            ),
            listOf(
                MediaType("foo", "*"),
                MediaType("*", "*", 0.1),
                MediaType("foo", "*", 0.5),
                MediaType("foo", "bar"),
                MediaType("*", "*"),
                MediaType("foo", "bar", 0.2)
            ).let { MediaType.prioritize(it) }
        )

        assertEquals(
            "sorted by qvalue descending",
            listOf(
                MediaType("audio", "basic"),
                MediaType("audio", "*", 0.2)
            ),
            MediaType.prioritize(
                listOf(
                    MediaType("audio", "*", 0.2),
                    MediaType("audio", "basic")
                )
            )
        )

        assertEquals(
            listOf(
                MediaType("text", "html"),
                MediaType("text", "x-c"),
                MediaType("text", "x-dvi", 0.8),
                MediaType("text", "plain", 0.5)
            ),
            listOf(
                MediaType("text", "plain", 0.5),
                MediaType("text", "html"),
                MediaType("text", "x-dvi", 0.8),
                MediaType("text", "x-c")
            ).let { MediaType.prioritize(it) }
        )

        assertEquals(
            listOf(
                MediaType("text", "html"),
                MediaType("text", "html", 0.7),
                MediaType("*", "*", 0.5),
                MediaType("text", "html", 0.4),
                MediaType("text", "*", 0.3)
            ),
            listOf(
                MediaType("text", "*", 0.3),
                MediaType("text", "html", 0.7),
                MediaType("text", "html"),
                MediaType("text", "html", 0.4),
                MediaType("*", "*", 0.5)
            ).let { MediaType.prioritize(it) }
        )

    }

    @Test
    fun testParsing1() {
        assertEquals(
            listOf(
                MediaType("audio", "*", 0.2),
                MediaType("audio", "basic")
            ),
            MediaType.parseHeader("audio/*; q=0.2, audio/basic")
        )
    }

    @Test
    fun testParsing2() {
        assertEquals(
            listOf(
                MediaType("text", "plain", 0.5),
                MediaType("text", "html"),
                MediaType("text", "x-dvi", 0.8),
                MediaType("text", "x-c")
            ),
            MediaType.parseHeader("text/plain; q=0.5, text/html, text/x-dvi; q=0.8, text/x-c")
        )
    }

    @Test
    fun testParsing3() {
        assertEquals(
            listOf(
                MediaType("text", "*", 0.3),
                MediaType("text", "html", 0.7),
                MediaType("text", "html"),
                MediaType("text", "html", 0.4),
                MediaType("*", "*", 0.5)
            ),
            MediaType.parseHeader("text/*;q=0.3, text/html;q=0.7, text/html;level=1, text/html;level=2;q=0.4, */*;q=0.5")
        )
    }

    @Test
    fun testAcceptable1() {
        assertEquals(
            "text" to "html",
            Request.toy().setHeader(Header.Accept, "*/*").negotiate.acceptableMediaType("text" to "html")
        )
    }

    @Test
    fun testAcceptable2() {
        assertEquals(
            "if header is absent, then it's assumed that client accepts all media types",
            "foo" to "bar",
            Request.toy().removeHeader(Header.Accept).negotiate.acceptableMediaType("foo" to "bar")
        )
    }
}












class NegotiatorTests {
    @Test
    fun testEncodingPriority() {
        val unsorted = listOf(
            Encoding("identity", 1.0),
            Encoding("identity", 0.8),
            Encoding("identity", 0.6),
            Encoding("a", 1.0),
            Encoding("b", 1.0),
            Encoding("c", 1.0),
            Encoding("b2", 0.8),
            Encoding("a2", 1.0),
            Encoding("c2", 0.6)
        )

        val expected = listOf(
            Encoding("a", 1.0),
            Encoding("b", 1.0),
            Encoding("c", 1.0),
            Encoding("a2", 1.0),
            Encoding("identity", 1.0),
            Encoding("b2", 0.8),
            Encoding("identity", 0.8),
            Encoding("c2", 0.6),
            Encoding("identity", 0.6)
        )

        assertEquals(
            "encodings are ranked by q-value, yet identity always comes last in their q-value bracket",
            expected.map(Encoding::name), Encoding.prioritize(unsorted).map(Encoding::name)
        )
    }

    @Test
    fun testEncoding1() {
        val neg = Negotiator(Request.toy().removeHeader(AcceptEncoding))
        assertEquals("when header is blank, identity always matches", "identity", neg.acceptableEncoding(listOf("identity")))
        assertEquals(null, neg.acceptableEncoding(listOf("gzip", "deflate")))
    }

    @Test
    fun testEncoding1a() {
        assertEquals("blank availables never matches",
            null,
            Negotiator(Request.toy().removeHeader(AcceptEncoding)).acceptableEncoding(listOf())
        )
        assertEquals("blank availables never matches",
            null,
            Negotiator(Request.toy().setHeader(AcceptEncoding, "gzip, deflate")).acceptableEncoding(listOf())
        )
    }


    @Test
    fun testEncoding2() {
        val neg = Negotiator(Request.toy().setHeader(AcceptEncoding, "identity"))
        assertEquals(
            "identity matches when it is specified",
            "identity", neg.acceptableEncoding(listOf("identity"))
        )
    }

    @Test
    fun testEncoding3() {
        val neg = Negotiator(Request.toy().setHeader(AcceptEncoding, "identity;q=0"))
        assertEquals(
            "identity is excluded, no matches",
            null, neg.acceptableEncoding(listOf("gzip", "identity", "deflate"))
        )
    }

    @Test
    fun testEncoding4() {
        val neg = Negotiator(Request.toy().setHeader(AcceptEncoding, "identity;q=0, gzip"))
        assertEquals(
            "gzip", neg.acceptableEncoding(listOf("identity", "deflate", "gzip"))
        )
    }

    @Test
    fun testEncoding5() {
        val neg = Negotiator(Request.toy().setHeader(AcceptEncoding, "*;q=0"))
        assertEquals(
            "nothing matches",
            null, neg.acceptableEncoding(listOf("identity", "deflate", "gzip"))
        )
    }

    @Test
    fun testEncoding6() {
        val neg = Negotiator(Request.toy().setHeader(AcceptEncoding, "*;q=0, gzip"))
        assertEquals(
            "an exception is made to 0-star",
            "gzip", neg.acceptableEncoding(listOf("identity", "deflate", "gzip"))
        )
    }

    @Test
    fun testEncoding7() {
        val neg = Negotiator(Request.toy().setHeader(AcceptEncoding, "gzip"))
        assertEquals(
            "prefers gzip",
            "gzip", neg.acceptableEncoding(listOf("identity", "deflate", "gzip"))
        )
    }
}
