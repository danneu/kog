package com.danneu.kog


val connect = { request: Request, socket: WebSocket ->
    val id = java.util.UUID.randomUUID()
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

val authenticateUser: Middleware = { handler -> fun(req: Request): Response {
    req.cookies["session_id"] != "xxx" && return Response(Status.forbidden)
    return handler(req)
}}

var checkRequest: Handler = authenticateUser({ Response(Status.switchingProtocols) })

//fun main(args: Array<String>) {
//    Server(onWebSocket = Pair(checkRequest, onConnect)).listen(3000)
//}


class Main {
    companion object {
        @JvmStatic fun main(args: Array<String>) {
            val handler: Handler = {
                Response()
                  .setHeader("Access-Control-Allow-Origin", "*")
                  .text("Hello, World!")
            }
            val server = Server(handler, onWebSocket = checkRequest to connect)
            server.listen(9000)
        }
    }
}

