package com.danneu.kog.toy

import com.danneu.kog.HeaderPair
import com.danneu.kog.Method
import com.danneu.kog.Protocol
import com.danneu.kog.Protocol.HTTP_1_1
import com.danneu.kog.Request
import javax.servlet.ReadListener
import javax.servlet.ServletInputStream

//
// Create Requests for testing.
//

// Lets us mock a Request with a noop input stream
class ToyStream: ServletInputStream() {
    override fun isReady(): Boolean = true
    override fun isFinished(): Boolean = true
    override fun read(): Int = -1
    override fun setReadListener(readListener: ReadListener?) {}
}

fun Request.Companion.toy(
    method: Method = Method.Get,
    path: String = "/",
    queryString: String = "foo=bar",
    headers: MutableList<HeaderPair> = mutableListOf(),
    protocol: Protocol = HTTP_1_1
): Request {
    return Request(
        serverPort = 3000,
        serverName = "name",
        remoteAddr = "1.2.3.4",
        href = path + if (queryString.isNotBlank()) { "?" + queryString } else { "" },
        queryString = queryString,
        scheme = "http",
        method = method,
        protocol = protocol,
        headers = headers,
        type = null,
        length = 0,
        charset = null,
        body = ToyStream(),
        path = path
    )
}
