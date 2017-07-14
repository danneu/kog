package com.danneu.kog

import com.danneu.kog.Protocol.Http_1_1
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
    protocol: Protocol = Http_1_1
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
        contentType = null,
        length = 0,
        body = ToyStream(),
        path = path
    )
}
