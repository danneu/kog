package com.danneu.kog

import com.danneu.kog.negotiation.Encoding
import com.danneu.kog.negotiation.MediaType
import org.junit.Assert.*
import org.junit.Test


class MediaTypeTests {
    @Test
    fun testDuplication() {
        assertEquals(
            "dupes are removed",
            listOf(MediaType("a", "b", 0.5)),
            Negotiator(Request.toy().setHeader(Header.Accept, "a/b;q=0.5, a/b;q=0.5, a/b;q=0.5")).mediaTypes
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
        val neg = Negotiator(Request.toy().removeHeader(Header.AcceptEncoding))
        assertEquals("when header is blank, identity always matches", "identity", neg.acceptableEncoding(listOf("identity")))
        assertEquals(null, neg.acceptableEncoding(listOf("gzip", "deflate")))
    }

    @Test
    fun testEncoding1a() {
        assertEquals("blank availables never matches",
            null,
            Negotiator(Request.toy().removeHeader(Header.AcceptEncoding)).acceptableEncoding(listOf())
        )
        assertEquals("blank availables never matches",
            null,
            Negotiator(Request.toy().setHeader(Header.AcceptEncoding, "gzip, deflate")).acceptableEncoding(listOf())
        )
    }


    @Test
    fun testEncoding2() {
        val neg = Negotiator(Request.toy().setHeader(Header.AcceptEncoding, "identity"))
        assertEquals(
            "identity matches when it is specified",
            "identity", neg.acceptableEncoding(listOf("identity"))
        )
    }

    @Test
    fun testEncoding3() {
        val neg = Negotiator(Request.toy().setHeader(Header.AcceptEncoding, "identity;q=0"))
        assertEquals(
            "identity is excluded, no matches",
            null, neg.acceptableEncoding(listOf("gzip", "identity", "deflate"))
        )
    }

    @Test
    fun testEncoding4() {
        val neg = Negotiator(Request.toy().setHeader(Header.AcceptEncoding, "identity;q=0, gzip"))
        assertEquals(
            "gzip", neg.acceptableEncoding(listOf("identity", "deflate", "gzip"))
        )
    }

    @Test
    fun testEncoding5() {
        val neg = Negotiator(Request.toy().setHeader(Header.AcceptEncoding, "*;q=0"))
        assertEquals(
            "nothing matches",
            null, neg.acceptableEncoding(listOf("identity", "deflate", "gzip"))
        )
    }

    @Test
    fun testEncoding6() {
        val neg = Negotiator(Request.toy().setHeader(Header.AcceptEncoding, "*;q=0, gzip"))
        assertEquals(
            "an exception is made to 0-star",
            "gzip", neg.acceptableEncoding(listOf("identity", "deflate", "gzip"))
        )
    }

    @Test
    fun testEncoding7() {
        val neg = Negotiator(Request.toy().setHeader(Header.AcceptEncoding, "gzip"))
        assertEquals(
            "prefers gzip",
            "gzip", neg.acceptableEncoding(listOf("identity", "deflate", "gzip"))
        )
    }
}
