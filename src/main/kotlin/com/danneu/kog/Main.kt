package com.danneu.kog

import org.eclipse.jetty.websocket.api.Session


val wsHandler = object : WebSocketHandler() {
    override fun onConnect(session: Session) {
        println("a client connected $session")
    }
    override fun onError(cause: Throwable) {
        println("onWebSocketError ${cause.message}")
    }
    override fun onClose(statusCode: Int, reason: String?) {
        println("onWebSocketClose $statusCode ${reason ?: "<no reason>"}")
    }
    override fun onText(message: String) {
        println("onWebSocketText $message")
    }
    override fun onBinary(payload: ByteArray, offset: Int, len: Int) {
        println("onWebSocketBinary")
    }
}


class Main {
    companion object {
        @JvmStatic fun main(args: Array<String>) {
            val handler: Handler = {
                Response()
                  .setHeader("Access-Control-Allow-Origin", "*")
                  .text("Hello, World!")
            }
            val server = Server(handler, websocket = wsHandler)
            server.listen(9000)
        }
    }
}

