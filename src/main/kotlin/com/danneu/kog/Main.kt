package com.danneu.kog

import java.util.UUID

fun socketHandler(socket: WebSocket) {
    val id = UUID.randomUUID()
    println("[$id] a client connected")

    socket.onError = { cause: Throwable ->
        println("[$id] onError ${cause.message}")
    }

    socket.onClose = { statusCode: Int, reason: String? ->
        println("[$id] onClose $statusCode ${reason ?: "<no reason>"}")
    }

    socket.onText = { message: String ->
        println("[$id] onText $message")
        socket.session.remote.sendString(message)
    }

    socket.onBinary = { payload: ByteArray, offset: Int, len: Int ->
        println("[$id] onBinary")
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
            val server = Server(handler, websocket = ::socketHandler)
            server.listen(9000)
        }
    }
}

