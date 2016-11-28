package com.danneu.kog

import org.junit.Assert.*
import org.junit.Test


class MethodTests {
    @Test
    fun testLookupByName() {
        assertEquals("lookup succeeds on exact caps", Method.Get, Method.fromString("Get"))
        assertEquals("lookup succeeds on different caps", Method.Options, Method.fromString("oPtIoNs"))
        assertEquals("returns Unknown when name is not recognized", Method.Unknown, Method.fromString("xxx"))
    }
}
