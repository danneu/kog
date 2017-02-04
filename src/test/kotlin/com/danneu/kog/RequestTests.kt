package com.danneu.kog

import org.junit.Assert.*
import org.junit.Test

fun upgradeRequest(protocol: String = "HTTP/1.1", method: Method = Method.Get): Request = Request.toy(
    protocol = protocol,
    method = method,
    headers = mutableListOf(
        Header.Upgrade to "websocket",
        Header.Connection to "upgrade"
    )
)

class RequestTests {
    @Test
    fun testIsUpgrade() {
        run {
            assertTrue("detects upgrade requests", upgradeRequest().isUpgrade())
        }

        // Method
        run {
            assertFalse("must be GET request", upgradeRequest(method = Method.Post).isUpgrade())
        }

        // Upgrade header
        run {
            assertFalse("must have header `Upgrade: websocket`", upgradeRequest().setHeader(Header.Upgrade, "not-websocket").isUpgrade())
            assertTrue("Upgrade header is case insensitive", upgradeRequest().setHeader(Header.Upgrade, "wEbSoCkEt").isUpgrade())
        }

        // Connection header
        run {
            assertFalse("connection header must contain 'upgrade'", upgradeRequest().setHeader(Header.Connection, "a, b, c").isUpgrade())
            assertTrue("handles list of values", upgradeRequest().setHeader(Header.Connection, "a, b, upgrade, c").isUpgrade())
            assertTrue("handles list of quoted values", upgradeRequest().setHeader(Header.Connection, "\"a, b\", \"upgrade\", c").isUpgrade())
        }

        // Protocol
        run {
            assertFalse("protocol must be HTTP/1.1", upgradeRequest(protocol = "HTTP/2.0").isUpgrade())
        }
    }
}

