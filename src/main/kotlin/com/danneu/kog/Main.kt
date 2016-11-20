package com.danneu.kog

import org.eclipse.jetty.websocket.api.Session
import java.util.Random



val socketHandler: () -> WebSocket = {
    object : WebSocket() {
        val id = Random().nextInt()
        override fun onConnect(session: Session) {
            println("[$id] a client connected")
        }
        override fun onError(cause: Throwable) {
            println("[$id] onError ${cause.message}")
        }
        override fun onClose(statusCode: Int, reason: String?) {
            println("[$id] onClose $statusCode ${reason ?: "<no reason>"}")
        }
        override fun onText(message: String) {
            println("[$id] onText $message")
        }
        override fun onBinary(payload: ByteArray, offset: Int, len: Int) {
            println("[$id] onBinary")
        }
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
            val server = Server(handler, websocket = socketHandler)
            server.listen(9000)
        }
    }
}

