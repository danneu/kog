package com.danneu.kog.json

import com.danneu.kog.json.Encoder as JE
import org.junit.Assert.*
import org.junit.Test

class EncoderTests {
    @Test
    fun testString() {
        assertEquals("\"42\"", JE.str("42").toString())
    }

    @Test
    fun testInt() {
        assertEquals("42", JE.num(42).toString())
    }

    @Test
    fun testLong() {
        assertEquals("42", JE.num(42.toLong()).toString())
    }

    @Test
    fun testShort() {
        assertEquals("42", JE.num(42.toShort()).toString())
    }

    @Test
    fun testFloat() {
        assertEquals("42", JE.num(42.toFloat()).toString())
    }

    @Test
    fun testDouble() {
        assertEquals("42", JE.num(42.toDouble()).toString())
    }

    @Test
    fun testBool() {
        assertEquals("true", JE.bool(true).toString())
        assertEquals("false", JE.bool(false).toString())
    }

    @Test
    fun testNull() {
        assertEquals("null", JE.`null`.toString())
    }

    @Test
    fun testArray() {
        assertEquals("[]", JE.array().toString())
        assertEquals("[[[]]]", JE.array(JE.array(JE.array())).toString())
        assertEquals(
            "[1,\"foo\",[2],null]",
            JE.array(JE.num(1), JE.str("foo"), JE.array(JE.num(2)), JE.`null`).toString()
        )
        assertEquals("handles sequence",
            "[1,[]]",
            JE.array(listOf(JE.num(1), JE.array()).asSequence()).toString()
        )
    }

    @Test
    fun testObject() {
        assertEquals("{}", JE.obj().toString())
        assertEquals("""{"a":{"b":{"c":{}}}}""", JE.obj("a" to JE.obj("b" to JE.obj("c" to JE.obj()))).toString())
        assertEquals(
            """{"a":1,"b":[],"c":null,"d":{}}""",
            JE.obj(
                "a" to JE.num(1),
                "b" to JE.array(),
                "c" to JE.`null`,
                "d" to JE.obj()
            ).toString()
        )
        assertEquals("handles sequence",
            """{"a":1,"b":[]}""",
            JE.obj(listOf("a" to JE.num(1), "b" to JE.array()).asSequence()).toString()
        )
    }

    @Test
    fun testMapToObject() {
        assertEquals("obj can be built from kotlin.Map",
            """{"a":1,"b":[{}]}""",
            JE.obj(mapOf(
                "a" to JE.num(1),
                "b" to JE.array(JE.obj())
            )).toString()
        )
    }
}


