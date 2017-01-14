package com.danneu.kog

import com.danneu.kog.negotiation.Encoding
import org.junit.Assert.*
import org.junit.Test

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
